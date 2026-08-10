package org.nooberic.dglib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.nooberic.dglib.dglib;
import org.nooberic.dglib.multiplayer.DgServerCoyoteApi;

public record DgC2SClientOpResponsePacket(
        long requestId,
        boolean success,
        boolean paired,
        int channelAStrength,
        int channelBStrength,
        int channelALimit,
        int channelBLimit,
        int channelAPainStrength,
        int channelBPainStrength,
        int channelASensationLowerLimit,
        int channelBSensationLowerLimit,
        String error
) implements CustomPacketPayload {
    public static final Type<DgC2SClientOpResponsePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dglib.MODID, "c2s_client_op_response"));

    public static final StreamCodec<FriendlyByteBuf, DgC2SClientOpResponsePacket> STREAM_CODEC = StreamCodec.of(
            DgC2SClientOpResponsePacket::encode,
            DgC2SClientOpResponsePacket::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public long getRequestId() { return requestId; }
    public boolean isSuccess() { return success; }
    public boolean isPaired() { return paired; }
    public int getChannelAStrength() { return channelAStrength; }
    public int getChannelBStrength() { return channelBStrength; }
    public int getChannelALimit() { return channelALimit; }
    public int getChannelBLimit() { return channelBLimit; }
    public int getChannelAPainStrength() { return channelAPainStrength; }
    public int getChannelBPainStrength() { return channelBPainStrength; }
    public int getChannelASensationLowerLimit() { return channelASensationLowerLimit; }
    public int getChannelBSensationLowerLimit() { return channelBSensationLowerLimit; }
    public String getError() { return error; }
    public static void encode(FriendlyByteBuf buf, DgC2SClientOpResponsePacket msg) {
        buf.writeLong(msg.requestId);
        buf.writeBoolean(msg.success);
        buf.writeBoolean(msg.paired);
        buf.writeInt(msg.channelAStrength);
        buf.writeInt(msg.channelBStrength);
        buf.writeInt(msg.channelALimit);
        buf.writeInt(msg.channelBLimit);
        buf.writeInt(msg.channelAPainStrength);
        buf.writeInt(msg.channelBPainStrength);
        buf.writeInt(msg.channelASensationLowerLimit);
        buf.writeInt(msg.channelBSensationLowerLimit);
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
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(1024)
        );
    }

    public static void handle(DgC2SClientOpResponsePacket msg, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sender) {
            ctx.enqueueWork(() -> DgServerCoyoteApi.get().handleClientResponse(sender, msg));
        }
    }
}
