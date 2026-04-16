package org.nooberic.dg_lib.multiplayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ServerCoyote {
    private final UUID playerId;
    private final DgServerCoyoteApi api;

    ServerCoyote(UUID playerId, DgServerCoyoteApi api) {
        this.playerId = playerId;
        this.api = api;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public CoyoteStatusSnapshot getCachedStatus() {
        return api.getCachedStatus(playerId);
    }

    public CompletableFuture<CoyoteStatusSnapshot> refreshStatus() {
        return api.requestStatus(playerId);
    }

    public CompletableFuture<Boolean> setStrength(int channel, int value) {
        return api.sendAndMapSuccess(playerId, org.nooberic.dg_lib.network.DgClientOperation.SET_STRENGTH, channel, value);
    }

    public CompletableFuture<Boolean> increaseStrength(int channel, int delta) {
        return api.sendAndMapSuccess(playerId, org.nooberic.dg_lib.network.DgClientOperation.INCREASE_STRENGTH, channel, delta);
    }

    public CompletableFuture<Boolean> decreaseStrength(int channel, int delta) {
        return api.sendAndMapSuccess(playerId, org.nooberic.dg_lib.network.DgClientOperation.DECREASE_STRENGTH, channel, delta);
    }

    public CompletableFuture<Boolean> playBasicWave(int channel, int seconds) {
        return api.sendAndMapSuccess(playerId, org.nooberic.dg_lib.network.DgClientOperation.PLAY_BASIC_WAVE, channel, seconds);
    }

    public CompletableFuture<Integer> getCurrentStrength(int channel) {
        return refreshStatus().thenApply(snapshot -> snapshot.currentStrength(channel));
    }

    public CompletableFuture<Integer> getStrengthLimit(int channel) {
        return refreshStatus().thenApply(snapshot -> snapshot.strengthLimit(channel));
    }
}
