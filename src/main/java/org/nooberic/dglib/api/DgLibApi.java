package org.nooberic.dglib.api;

import net.minecraft.server.level.ServerPlayer;
import org.nooberic.dglib.client.control.WaveControlScheduler;
import org.nooberic.dglib.pulse.Pulse;
import org.nooberic.dglib.pulse.PulseRegistry;
import org.nooberic.dglib.pulse.RegisteredPulse;
import org.nooberic.dglib.service.ConnectionState;
import org.nooberic.dglib.service.DeviceStatus;
import org.nooberic.dglib.service.DgLabService;
import org.nooberic.dglib.service.DgLabServiceImpl;
import org.nooberic.dglib.service.StrengthFeedbackListener;
import org.nooberic.dglib.util.ElectricityParticleUtil;
import org.nooberic.dglib.util.SoftStrengthConverter;

import java.util.Map;

public final class DgLibApi implements IDgLibApi {
    private static final DgLibApi INSTANCE = new DgLibApi();

    private final DgLabService service;
    private final WaveControlScheduler scheduler;

    private DgLibApi() {
        this.service = new DgLabServiceImpl();
        this.scheduler = new WaveControlScheduler(this.service);
    }

    public static DgLibApi get() {
        return INSTANCE;
    }

    @Override
    public void initialize() {
        scheduler.initialize();
        service.initialize();
    }

    @Override
    public void shutdown() {
        scheduler.shutdown();
        service.shutdown();
    }

    @Override
    public void connect() {
        service.connect();
    }

    @Override
    public void disconnect() {
        service.disconnect();
    }

    @Override
    public ConnectionState getConnectionState() {
        return service.getConnectionState();
    }

    @Override
    public boolean isPaired() {
        return service.isPaired();
    }

    @Override
    public DeviceStatus getStatus() {
        return service.getStatus();
    }

    @Override
    public int getCurrentStrength(int channel) {
        return service.getCurrentStrength(channel);
    }

    @Override
    public int getStrengthLimit(int channel) {
        return service.getStrengthLimit(channel);
    }

    @Override
    public int getPainStrength(int channel) {
        return service.getPainStrength(channel);
    }

    @Override
    public void setPainStrength(int channel, int value) {
        service.setPainStrength(channel, value);
    }

    @Override
    public int getSensationLowerLimit(int channel) {
        return service.getSensationLowerLimit(channel);
    }

    @Override
    public void setSensationLowerLimit(int channel, int value) {
        service.setSensationLowerLimit(channel, value);
    }

    @Override
    public void setStrengthFeedbackListener(StrengthFeedbackListener listener) {
        service.setStrengthFeedbackListener(listener);
    }

    @Override
    public boolean increaseStrength(int channel, int delta) {
        return service.increaseStrength(channel, delta);
    }

    @Override
    public boolean decreaseStrength(int channel, int delta) {
        return service.decreaseStrength(channel, delta);
    }

    @Override
    public boolean setStrength(int channel, int value) {
        if (channel == 3) {
            boolean channelAOk = service.setStrength(1, value);
            boolean channelBOk = service.setStrength(2, value);
            return channelAOk && channelBOk;
        }
        return service.setStrength(channel, value);
    }

    @Override
    public boolean setSoftStrength(int channel, int softValue) {
        if (channel == 3) {
            boolean channelAOk = setSoftStrength(1, softValue);
            boolean channelBOk = setSoftStrength(2, softValue);
            return channelAOk && channelBOk;
        }
        return setStrength(channel, SoftStrengthConverter.toRealStrength(channel, softValue));
    }

    @Override
    public boolean clear(int channel) {
        scheduler.clear(channel);
        return true;
    }

    @Override
    public boolean control(int channel, int strength, String pulseId, int seconds) {
        RegisteredPulse pulse = PulseRegistry.get(pulseId);
        if (pulse == null) {
            return false;
        }
        if (channel == 3) {
            boolean channelAOk = scheduler.schedule(1, strength, pulse, seconds);
            boolean channelBOk = scheduler.schedule(2, strength, pulse, seconds);
            return channelAOk && channelBOk;
        }
        return scheduler.schedule(channel, strength, pulse, seconds);
    }

    @Override
    public boolean controlSoft(int channel, int softStrength, String pulseId, int seconds) {
        if (channel == 3) {
            RegisteredPulse pulse = PulseRegistry.get(pulseId);
            if (pulse == null) {
                return false;
            }
            boolean channelAOk = scheduler.schedule(1, SoftStrengthConverter.toRealStrength(1, softStrength), pulse, seconds);
            boolean channelBOk = scheduler.schedule(2, SoftStrengthConverter.toRealStrength(2, softStrength), pulse, seconds);
            return channelAOk && channelBOk;
        }
        return control(channel, SoftStrengthConverter.toRealStrength(channel, softStrength), pulseId, seconds);
    }

    @Override
    public boolean playBasicWave(int channel, int seconds) {
        if (channel == 3) {
            boolean channelAOk = service.playBasicWave(1, seconds);
            boolean channelBOk = service.playBasicWave(2, seconds);
            return channelAOk && channelBOk;
        }
        return service.playBasicWave(channel, seconds);
    }

    @Override
    public boolean playPulse(Pulse pulse) {
        return service.playPulse(pulse);
    }

    public boolean playPulse(int channel, RegisteredPulse pulse, int seconds) {
        if (pulse == null) {
            return false;
        }
        int safeSeconds = Math.max(1, Math.min(60, seconds));
        if (channel == 3) {
            boolean channelAOk = playPulse(1, pulse, safeSeconds);
            boolean channelBOk = playPulse(2, pulse, safeSeconds);
            return channelAOk && channelBOk;
        }
        Pulse runtimePulse = new Pulse(
            channel == 2 ? Pulse.Channel.B : Pulse.Channel.A,
            safeSeconds,
            pulse.getFrames()
        );
        return playPulse(runtimePulse);
    }

    @Override
    public boolean registerPulse(String id, String pulseFileName) {
        return PulseRegistry.registerFromResource(id, pulseFileName);
    }

    @Override
    public void spawnElectricityParticles(ServerPlayer player) {
        ElectricityParticleUtil.spawnAroundPlayer(player);
    }

    @Override
    public void spawnElectricityParticles(ServerPlayer player, int particleCount) {
        ElectricityParticleUtil.spawnAroundPlayer(player, particleCount);
    }

    @Override
    public RegisteredPulse getPulse(String id) {
        return PulseRegistry.get(id);
    }

    @Override
    public Map<String, RegisteredPulse> getAllPulses() {
        return PulseRegistry.all();
    }
}
