package org.nooberic.dg_lib.multiplayer;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.nooberic.dg_lib.network.DgC2SClientOpResponsePacket;
import org.nooberic.dg_lib.network.DgClientOperation;
import org.nooberic.dg_lib.network.DgNetworking;
import org.nooberic.dg_lib.network.DgS2CClientOpRequestPacket;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public final class DgServerCoyoteApi {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DgServerCoyoteApi INSTANCE = new DgServerCoyoteApi();
    private static final long REQUEST_TIMEOUT_MS = 5000L;

    private final AtomicLong requestSequence = new AtomicLong(1L);
    private final ConcurrentMap<UUID, ServerCoyote> coyotes = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, CoyoteStatusSnapshot> statusCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PendingRequest> pending = new ConcurrentHashMap<>();

    private DgServerCoyoteApi() {
    }

    public static DgServerCoyoteApi get() {
        return INSTANCE;
    }

    public ServerCoyote getCoyote(ServerPlayer player) {
        return coyotes.computeIfAbsent(player.getUUID(), id -> new ServerCoyote(id, this));
    }

    public CoyoteStatusSnapshot getCachedStatus(UUID playerId) {
        return statusCache.getOrDefault(playerId, CoyoteStatusSnapshot.empty());
    }

    public CompletableFuture<CoyoteStatusSnapshot> requestStatus(UUID playerId) {
        return send(playerId, DgClientOperation.QUERY_STATUS, 1, 0);
    }

    public CompletableFuture<Boolean> sendAndMapSuccess(UUID playerId, DgClientOperation operation, int channel, int value) {
        return send(playerId, operation, channel, value).thenApply(snapshot -> snapshot.getError().isEmpty());
    }

    public void handleClientResponse(ServerPlayer sender, DgC2SClientOpResponsePacket packet) {
        PendingRequest req = pending.remove(packet.getRequestId());
        if (req == null) {
            return;
        }

        if (!sender.getUUID().equals(req.playerId)) {
            req.future.completeExceptionally(new IllegalStateException("response-player-mismatch"));
            return;
        }

        String error = packet.isSuccess() ? "" : packet.getError();
        CoyoteStatusSnapshot snapshot = new CoyoteStatusSnapshot(
                packet.isPaired(),
                packet.getChannelAStrength(),
                packet.getChannelBStrength(),
                packet.getChannelALimit(),
                packet.getChannelBLimit(),
            packet.getChannelAPainStrength(),
            packet.getChannelBPainStrength(),
            packet.getChannelASensationLowerLimit(),
            packet.getChannelBSensationLowerLimit(),
                error,
                System.currentTimeMillis()
        );
        statusCache.put(req.playerId, snapshot);
        req.future.complete(snapshot);
    }

    public void onPlayerLogout(ServerPlayer player) {
        UUID playerId = player.getUUID();
        coyotes.remove(playerId);
        statusCache.remove(playerId);

        pending.forEach((id, req) -> {
            if (req.playerId.equals(playerId) && pending.remove(id, req)) {
                req.future.completeExceptionally(new IllegalStateException("player-logout"));
            }
        });
    }

    private CompletableFuture<CoyoteStatusSnapshot> send(UUID playerId, DgClientOperation operation, int channel, int value) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer() == null
                ? null
                : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);

        if (player == null) {
            CompletableFuture<CoyoteStatusSnapshot> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("player-offline"));
            return failed;
        }

        long requestId = requestSequence.getAndIncrement();
        CompletableFuture<CoyoteStatusSnapshot> future = new CompletableFuture<>();
        pending.put(requestId, new PendingRequest(playerId, future));

        DgNetworking.sendToPlayer(player, new DgS2CClientOpRequestPacket(requestId, operation, normalizeChannel(channel), value));

        CompletableFuture.delayedExecutor(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS).execute(() -> {
            PendingRequest req = pending.remove(requestId);
            if (req != null) {
                req.future.completeExceptionally(new TimeoutException("client-op-timeout"));
            }
        });

        return future;
    }

    private int normalizeChannel(int channel) {
        return channel == 2 ? 2 : 1;
    }

    private static final class PendingRequest {
        private final UUID playerId;
        private final CompletableFuture<CoyoteStatusSnapshot> future;

        private PendingRequest(UUID playerId, CompletableFuture<CoyoteStatusSnapshot> future) {
            this.playerId = playerId;
            this.future = future;
        }
    }
}
