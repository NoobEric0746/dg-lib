package org.nooberic.dglib.coyote.api;

import net.minecraft.server.level.ServerPlayer;
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.multiplayer.CoyoteStatusSnapshot;
import org.nooberic.dglib.multiplayer.DgServerCoyoteApi;
import org.nooberic.dglib.pulse.Pulse;
import org.nooberic.dglib.pulse.RegisteredPulse;
import org.nooberic.dglib.service.ConnectionState;
import org.nooberic.dglib.service.DeviceStatus;

import java.util.concurrent.CompletableFuture;

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

    public static boolean playPulse(int channel, RegisteredPulse pulse, int seconds) {
        return DgLibApi.get().playPulse(channel, pulse, seconds);
    }

    public static ConnectionState getConnectionState() {
        return DgLibApi.get().getConnectionState();
    }

    public static boolean isPaired() {
        return DgLibApi.get().isPaired();
    }

    public static DeviceStatus getStatus() {
        return DgLibApi.get().getStatus();
    }

    public static CompletableFuture<CoyoteStatusSnapshot> queryPlayerStatus(ServerPlayer player) {
        return DgServerCoyoteApi.get().getCoyote(player).refreshStatus();
    }

    public static boolean isPlayerPaired(ServerPlayer player) {
        return DgServerCoyoteApi.get().getCoyote(player).getCachedStatus().isPaired();
    }

    public static CoyoteStatusSnapshot getPlayerStatusSnapshot(ServerPlayer player) {
        return DgServerCoyoteApi.get().getCoyote(player).getCachedStatus();
    }
}
