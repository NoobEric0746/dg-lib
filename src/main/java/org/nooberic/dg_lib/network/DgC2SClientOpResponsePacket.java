package org.nooberic.dg_lib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.nooberic.dg_lib.multiplayer.DgServerCoyoteApi;

import java.util.function.Supplier;

public class DgC2SClientOpResponsePacket {
    private final long requestId;
    private final boolean success;
    private final boolean paired;
    private final int channelAStrength;
    private final int channelBStrength;
    private final int channelALimit;
    private final int channelBLimit;
    private final String error;

    public DgC2SClientOpResponsePacket(
            long requestId,
            boolean success,
            boolean paired,
            int channelAStrength,
            int channelBStrength,
            int channelALimit,
            int channelBLimit,
            String error
    ) {
        this.requestId = requestId;
        this.success = success;
        this.paired = paired;
        this.channelAStrength = channelAStrength;
        this.channelBStrength = channelBStrength;
        this.channelALimit = channelALimit;
        this.channelBLimit = channelBLimit;
        this.error = error == null ? "" : error;
    }

    public long getRequestId() {
        return requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isPaired() {
        return paired;
    }

    public int getChannelAStrength() {
        return channelAStrength;
    }

    public int getChannelBStrength() {
        return channelBStrength;
    }

    public int getChannelALimit() {
        return channelALimit;
    }

    public int getChannelBLimit() {
        return channelBLimit;
    }

    public String getError() {
        return error;
    }

    public static void encode(DgC2SClientOpResponsePacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.requestId);
        buf.writeBoolean(msg.success);
        buf.writeBoolean(msg.paired);
        buf.writeInt(msg.channelAStrength);
        buf.writeInt(msg.channelBStrength);
        buf.writeInt(msg.channelALimit);
        buf.writeInt(msg.channelBLimit);
        buf.writeUtf(msg.error);
    }

    public static DgC2SClientOpResponsePacket decode(FriendlyByteBuf buf) {
        return new DgC2SClientOpResponsePacket(
                buf.readLong(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(1024)
        );
    }

    public static void handle(DgC2SClientOpResponsePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer sender = ctx.getSender();
        if (sender != null) {
            ctx.enqueueWork(() -> DgServerCoyoteApi.get().handleClientResponse(sender, msg));
        }
        ctx.setPacketHandled(true);
    }
}
