package org.nooberic.dg_lib.service;

public interface DgLabService {
    void initialize();

    void shutdown();

    void connect();

    void disconnect();

    ConnectionState getConnectionState();

    boolean isPaired();

    DeviceStatus getStatus();

    boolean increaseStrength(int channel, int delta);

    boolean decreaseStrength(int channel, int delta);

    boolean setStrength(int channel, int value);
}
