package org.nooberic.dglib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.nooberic.dglib.dglib;

public record DgS2CClientOpRequestPacket(long requestId, DgClientOperation operation, int channel, int value, String payload) implements CustomPacketPayload {
    public static final Type<DgS2CClientOpRequestPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dglib.MODID, "s2c_client_op_request"));

    public DgS2CClientOpRequestPacket(long requestId, DgClientOperation operation, int channel, int value) {
        this(requestId, operation, channel, value, "");
    }

    public static final StreamCodec<FriendlyByteBuf, DgS2CClientOpRequestPacket> STREAM_CODEC = StreamCodec.of(
            DgS2CClientOpRequestPacket::encode,
            DgS2CClientOpRequestPacket::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public long getRequestId() { return requestId; }
    public DgClientOperation getOperation() { return operation; }
    public int getChannel() { return channel; }
    public int getValue() { return value; }
    public String getPayload() { return payload; }
    public static void encode(FriendlyByteBuf buf, DgS2CClientOpRequestPacket msg) {
        buf.writeLong(msg.requestId);
        buf.writeInt(msg.operation.id());
        buf.writeInt(msg.channel);
        buf.writeInt(msg.value);
        buf.writeUtf(msg.payload, 256);
    }

    public static DgS2CClientOpRequestPacket decode(FriendlyByteBuf buf) {
        long requestId = buf.readLong();
        DgClientOperation operation = DgClientOperation.fromId(buf.readInt());
        int channel = buf.readInt();
        int value = buf.readInt();
        String payload = buf.readUtf(256);
        return new DgS2CClientOpRequestPacket(requestId, operation, channel, value, payload);
    }

    public static void handle(DgS2CClientOpRequestPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> DgClientPacketExecutor.handleRequest(msg));
    }
}
