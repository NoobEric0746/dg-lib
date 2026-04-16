package org.nooberic.dg_lib.service;

public interface DgLabService {
    void initialize();

    void shutdown();

    void connect();

    void disconnect();

    ConnectionState getConnectionState();

    boolean isPaired();

    DeviceStatus getStatus();

    int getCurrentStrength(int channel);

    int getStrengthLimit(int channel);

    int getPainStrength(int channel);

    void setPainStrength(int channel, int value);

    int getSensationLowerLimit(int channel);

    void setSensationLowerLimit(int channel, int value);

    void setStrengthFeedbackListener(StrengthFeedbackListener listener);

    boolean increaseStrength(int channel, int delta);

    boolean decreaseStrength(int channel, int delta);

    boolean setStrength(int channel, int value);

    boolean playBasicWave(int channel, int seconds);
}
