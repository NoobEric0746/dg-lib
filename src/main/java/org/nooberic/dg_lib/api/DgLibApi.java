package org.nooberic.dg_lib.api;

import org.nooberic.dg_lib.pulse.Pulse;
import org.nooberic.dg_lib.pulse.PulseRegistry;
import org.nooberic.dg_lib.service.ConnectionState;
import org.nooberic.dg_lib.service.DeviceStatus;
import org.nooberic.dg_lib.service.DgLabService;
import org.nooberic.dg_lib.service.DgLabServiceImpl;
import org.nooberic.dg_lib.service.StrengthFeedbackListener;

import java.util.Map;

public final class DgLibApi implements IDgLibApi {
    private static final DgLibApi INSTANCE = new DgLibApi();

    private final DgLabService service;

    private DgLibApi() {
        this.service = new DgLabServiceImpl();
    }

    public static DgLibApi get() {
        return INSTANCE;
    }

    @Override
    public void initialize() {
        service.initialize();
    }

    @Override
    public void shutdown() {
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
        return service.setStrength(channel, value);
    }

    @Override
    public boolean playBasicWave(int channel, int seconds) {
        return service.playBasicWave(channel, seconds);
    }

    @Override
    public boolean playPulse(Pulse pulse) {
        return service.playPulse(pulse);
    }

    @Override
    public boolean registerPulse(String id, String pulseFileName) {
        return PulseRegistry.registerFromResource(id, pulseFileName);
    }

    @Override
    public Pulse getPulse(String id) {
        return PulseRegistry.get(id);
    }

    @Override
    public Map<String, Pulse> getAllPulses() {
        return PulseRegistry.all();
    }
}
