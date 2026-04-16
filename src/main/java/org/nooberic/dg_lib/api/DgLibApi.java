package org.nooberic.dg_lib.api;

import org.nooberic.dg_lib.service.ConnectionState;
import org.nooberic.dg_lib.service.DeviceStatus;
import org.nooberic.dg_lib.service.DgLabService;
import org.nooberic.dg_lib.service.DgLabServiceImpl;

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
}
