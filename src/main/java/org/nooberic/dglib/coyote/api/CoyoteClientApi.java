package org.nooberic.dglib.coyote.api;

import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.pulse.Pulse;

public final class CoyoteClientApi {
    private CoyoteClientApi() {
    }

    public static void connect() {
        DgLibApi.get().connect();
    }

    public static void disconnect() {
        DgLibApi.get().disconnect();
    }

    public static boolean setStrength(int channel, int value) {
        return DgLibApi.get().setStrength(channel, value);
    }

    public static boolean control(int channel, int strength, String pulseId, int seconds) {
        return DgLibApi.get().control(channel, strength, pulseId, seconds);
    }

    public static boolean playPulse(Pulse pulse) {
        return DgLibApi.get().playPulse(pulse);
    }
}
