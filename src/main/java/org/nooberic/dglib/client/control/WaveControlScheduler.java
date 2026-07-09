package org.nooberic.dglib.client.control;

import org.nooberic.dglib.pulse.Pulse;
import org.nooberic.dglib.service.DgLabService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class WaveControlScheduler {
    private static final int MIN_SECONDS = 1;
    private static final int MAX_SECONDS = 60;
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

    public synchronized boolean schedule(int channel, int strength, Pulse pulse, int seconds) {
        if (pulse == null) {
            return false;
        }
        ensureExecutor();

        int normalizedChannel = channel == 2 ? 2 : 1;
        int safeSeconds = clampSeconds(seconds);
        long durationMillis = secondsToMillis(safeSeconds);
        long sequence = sequenceGenerator.incrementAndGet();
        WaveEvent event = new WaveEvent(
                normalizedChannel,
                strength,
                rebuildPulse(pulse, normalizedChannel, safeSeconds),
                durationMillis,
                sequence
        );

        ChannelState state = channelStates.computeIfAbsent(normalizedChannel, ignored -> new ChannelState());
        if (state.activeEvent == null) {
            return startEvent(state, event);
        }

        if (event.strength >= state.activeEvent.strength) {
            stopActiveEvent(state);
            return startEvent(state, event);
        }

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

    private void stopActiveEvent(ChannelState state) {
        WaveEvent active = state.activeEvent;
        if (active == null) {
            return;
        }
        state.activeEvent = null;
        state.generation++;
    }

    private synchronized void finishEvent(int channel, long generation) {
        ChannelState state = channelStates.get(channel);
        if (state == null || state.generation != generation) {
            return;
        }

        state.activeEvent = null;
        service.clearScheduledControlState(channel);
        service.setStrength(channel, 0);
        service.hardClear(channel);
    }

    private void clearChannel(int channel) {
        ChannelState state = channelStates.computeIfAbsent(channel, ignored -> new ChannelState());
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

    private int clampSeconds(int seconds) {
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
    }

    private long secondsToMillis(int seconds) {
        return (long) clampSeconds(seconds) * 1_000L;
    }

    private int millisToWireSeconds(long durationMillis) {
        long clampedMillis = Math.max(secondsToMillis(MIN_SECONDS), durationMillis);
        return (int) Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, (clampedMillis + 999L) / 1_000L));
    }

    private static final class ChannelState {
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
