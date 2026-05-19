package org.nooberic.dglib.client.control;

import org.nooberic.dglib.pulse.Pulse;
import org.nooberic.dglib.service.DgLabService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class WaveControlScheduler {
    private static final double MIN_SECONDS = 0.1D;
    private static final double MAX_SECONDS = 60D;
    private static final long MIN_DURATION_MILLIS = 100L;
    private static final long FINISH_BUFFER_MILLIS = 150L;

    private final DgLabService service;
    private final AtomicLong sequenceGenerator;
    private final Map<Integer, ChannelState> channelStates;
    private ScheduledExecutorService executor;

    public WaveControlScheduler(DgLabService service) {
        this.service = service;
        this.sequenceGenerator = new AtomicLong(0L);
        this.channelStates = new HashMap<>();
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public synchronized boolean schedule(int channel, int strength, Pulse pulse, double seconds) {
        if (pulse == null) {
            return false;
        }
        ensureExecutor();

        int normalizedChannel = channel == 2 ? 2 : 1;
        double safeSeconds = clampSeconds(seconds);
        long durationMillis = secondsToMillis(safeSeconds);
        long sequence = sequenceGenerator.incrementAndGet();
        WaveEvent event = new WaveEvent(
                normalizedChannel,
                strength,
                rebuildPulse(pulse, normalizedChannel, millisToWireSeconds(durationMillis)),
                durationMillis,
                sequence
        );

        ChannelState state = channelStates.computeIfAbsent(normalizedChannel, ignored -> new ChannelState());
        if (state.activeEvent == null) {
            return startEvent(state, event);
        }

        if (comparePriority(event, state.activeEvent) < 0) {
            pauseActiveEvent(state);
            return startEvent(state, event);
        }

        state.waitingEvents.add(event);
        return true;
    }

    public synchronized void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
        channelStates.clear();
    }

    public synchronized void initialize() {
        ensureExecutor();
    }

    public synchronized void clear(int channel) {
        if (channel == 3) {
            clearChannel(1);
            clearChannel(2);
            return;
        }
        clearChannel(channel == 2 ? 2 : 1);
    }

    private Pulse rebuildPulse(Pulse pulse, int channel, int seconds) {
        return new Pulse(
                pulse.getName(),
                channel == 2 ? Pulse.Channel.B : Pulse.Channel.A,
                seconds,
                pulse.getFrames()
        );
    }

    private int comparePriority(WaveEvent left, WaveEvent right) {
        if (left.strength != right.strength) {
            return Integer.compare(right.strength, left.strength);
        }
        return Long.compare(right.sequence, left.sequence);
    }

    private boolean startEvent(ChannelState state, WaveEvent event) {
        if (!service.control(event.channel, event.strength, event.pulse)) {
            return false;
        }
        long generation = ++state.generation;
        event.startedAtMillis = System.currentTimeMillis();
        state.activeEvent = event;
        event.pulse = rebuildPulse(event.pulse, event.channel, millisToWireSeconds(event.remainingMillis));
        try {
            executor.schedule(
                    () -> finishEvent(event.channel, generation),
                    event.remainingMillis + FINISH_BUFFER_MILLIS,
                    TimeUnit.MILLISECONDS
            );
        } catch (RejectedExecutionException ex) {
            state.activeEvent = null;
            service.hardClear(event.channel);
            return false;
        }
        return true;
    }

    private void pauseActiveEvent(ChannelState state) {
        WaveEvent active = state.activeEvent;
        if (active == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long elapsedMillis = Math.max(0L, now - active.startedAtMillis);
        long remainingMillis = Math.max(MIN_DURATION_MILLIS, active.remainingMillis - elapsedMillis);
        active.remainingMillis = remainingMillis;
        active.startedAtMillis = 0L;
        service.clearScheduledControlState(active.channel);
        service.hardClear(active.channel);
        state.waitingEvents.add(active);
        state.activeEvent = null;
        state.generation++;
    }

    private synchronized void finishEvent(int channel, long generation) {
        ChannelState state = channelStates.get(channel);
        if (state == null || state.generation != generation) {
            return;
        }

        state.activeEvent = null;
        WaveEvent next = pollNextEvent(state);
        if (next != null) {
            startEvent(state, next);
            return;
        }

        service.clearScheduledControlState(channel);
        service.setStrength(channel, 0);
        service.hardClear(channel);
    }

    private WaveEvent pollNextEvent(ChannelState state) {
        if (state.waitingEvents.isEmpty()) {
            return null;
        }
        List<WaveEvent> candidates = new ArrayList<>(state.waitingEvents);
        candidates.sort(this::comparePriority);
        WaveEvent next = candidates.get(0);
        state.waitingEvents.remove(next);
        return next;
    }

    private void clearChannel(int channel) {
        ChannelState state = channelStates.computeIfAbsent(channel, ignored -> new ChannelState());
        state.waitingEvents.clear();
        state.activeEvent = null;
        state.generation++;
        service.clearScheduledControlState(channel);
        service.hardClear(channel);
    }

    private void ensureExecutor() {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newSingleThreadScheduledExecutor();
        }
    }

    private double clampSeconds(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            return MIN_SECONDS;
        }
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
    }

    private long secondsToMillis(double seconds) {
        return Math.max(MIN_DURATION_MILLIS, Math.round(clampSeconds(seconds) * 1_000D));
    }

    private int millisToWireSeconds(long durationMillis) {
        long clampedMillis = Math.max(MIN_DURATION_MILLIS, durationMillis);
        return (int) Math.max(1L, Math.min(60L, (clampedMillis + 999L) / 1_000L));
    }

    private static final class ChannelState {
        private final List<WaveEvent> waitingEvents = new ArrayList<>();
        private WaveEvent activeEvent;
        private long generation;
    }

    private static final class WaveEvent {
        private final int channel;
        private final int strength;
        private final long sequence;
        private Pulse pulse;
        private long remainingMillis;
        private long startedAtMillis;

        private WaveEvent(int channel, int strength, Pulse pulse, long remainingMillis, long sequence) {
            this.channel = channel;
            this.strength = strength;
            this.pulse = pulse;
            this.remainingMillis = remainingMillis;
            this.sequence = sequence;
            this.startedAtMillis = 0L;
        }
    }
}
