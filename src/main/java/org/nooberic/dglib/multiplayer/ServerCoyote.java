package org.nooberic.dglib.multiplayer;

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
        return api.sendAndMapSuccess(playerId, org.nooberic.dglib.network.DgClientOperation.SET_STRENGTH, channel, value);
    }

    public CompletableFuture<Boolean> setSoftStrength(int channel, int softValue) {
        return api.sendAndMapSuccess(playerId, org.nooberic.dglib.network.DgClientOperation.SET_SOFT_STRENGTH, channel, softValue);
    }

    public CompletableFuture<Boolean> clear(int channel) {
        return api.sendAndMapSuccess(playerId, org.nooberic.dglib.network.DgClientOperation.CLEAR, channel, 0);
    }

    public CompletableFuture<Boolean> increaseStrength(int channel, int delta) {
        return api.sendAndMapSuccess(playerId, org.nooberic.dglib.network.DgClientOperation.INCREASE_STRENGTH, channel, delta);
    }

    public CompletableFuture<Boolean> decreaseStrength(int channel, int delta) {
        return api.sendAndMapSuccess(playerId, org.nooberic.dglib.network.DgClientOperation.DECREASE_STRENGTH, channel, delta);
    }

    public CompletableFuture<Boolean> playBasicWave(int channel, int seconds) {
        return api.sendAndMapSuccess(playerId, org.nooberic.dglib.network.DgClientOperation.PLAY_BASIC_WAVE, channel, seconds);
    }

    public CompletableFuture<Boolean> playPulseById(String pulseId, int seconds) {
        return api.sendAndMapSuccess(playerId, org.nooberic.dglib.network.DgClientOperation.PLAY_PULSE_BY_ID, seconds, pulseId);
    }

    public CompletableFuture<Boolean> control(int channel, int strength, String pulseId, double seconds) {
        String payload = strength + "|" + pulseId;
        int encodedTenths = (int) Math.max(1L, Math.min(600L, Math.round(seconds * 10.0D)));
        return api.sendAndMapSuccess(playerId, org.nooberic.dglib.network.DgClientOperation.CONTROL, channel, encodedTenths, payload);
    }

    public CompletableFuture<Boolean> controlSoft(int channel, int softStrength, String pulseId, double seconds) {
        String payload = softStrength + "|" + pulseId;
        int encodedTenths = (int) Math.max(1L, Math.min(600L, Math.round(seconds * 10.0D)));
        return api.sendAndMapSuccess(playerId, org.nooberic.dglib.network.DgClientOperation.CONTROL_SOFT, channel, encodedTenths, payload);
    }

    public CompletableFuture<Integer> getCurrentStrength(int channel) {
        return refreshStatus().thenApply(snapshot -> snapshot.currentStrength(channel));
    }

    public CompletableFuture<Integer> getStrengthLimit(int channel) {
        return refreshStatus().thenApply(snapshot -> snapshot.strengthLimit(channel));
    }
}
