package org.nooberic.dg_lib.api;

import org.nooberic.dg_lib.service.ConnectionState;
import org.nooberic.dg_lib.service.DeviceStatus;

public interface IDgLibApi {
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

    boolean playBasicWave(int channel, int seconds);
}
