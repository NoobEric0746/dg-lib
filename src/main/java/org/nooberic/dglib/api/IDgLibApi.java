package org.nooberic.dglib.api;

import org.nooberic.dglib.pulse.Pulse;
import org.nooberic.dglib.service.ConnectionState;
import org.nooberic.dglib.service.DeviceStatus;
import org.nooberic.dglib.service.StrengthFeedbackListener;

import java.util.Map;

public interface IDgLibApi {
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

    boolean control(int channel, int strength, String pulseId, int seconds);

    boolean playBasicWave(int channel, int seconds);

    boolean playPulse(Pulse pulse);

    boolean registerPulse(String id, String pulseFileName);

    Pulse getPulse(String id);

    Map<String, Pulse> getAllPulses();
}
