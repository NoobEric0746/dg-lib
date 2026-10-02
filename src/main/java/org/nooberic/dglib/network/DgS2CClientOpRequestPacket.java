package org.nooberic.dglib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.nooberic.dglib.dglib;

public class DgS2CClientOpRequestPacket implements CustomPacketPayload {
    public static final Type<DgS2CClientOpRequestPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(dglib.MODID, "client_op_request"));
    public static final StreamCodec<FriendlyByteBuf, DgS2CClientOpRequestPacket> STREAM_CODEC =
            StreamCodec.of((buffer, message) -> encode(message, buffer), DgS2CClientOpRequestPacket::decode);

    @Override
    public Type<DgS2CClientOpRequestPacket> type() {
        return TYPE;
    }

    private final long requestId;
    private final DgClientOperation operation;
    private final int channel;
    private final int value;
    private final String payload;

    public DgS2CClientOpRequestPacket(long requestId, DgClientOperation operation, int channel, int value) {
        this(requestId, operation, channel, value, "");
    }

    public DgS2CClientOpRequestPacket(long requestId, DgClientOperation operation, int channel, int value, String payload) {
        this.requestId = requestId;
        this.operation = operation;
        this.channel = channel;
        this.value = value;
        this.payload = payload == null ? "" : payload;
    }

    public long getRequestId() {
        return requestId;
    }

    public DgClientOperation getOperation() {
        return operation;
    }

    public int getChannel() {
        return channel;
    }

    public int getValue() {
        return value;
    }

    public String getPayload() {
        return payload;
    }

    public static void encode(DgS2CClientOpRequestPacket msg, FriendlyByteBuf buf) {
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
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ctx.enqueueWork(() -> DgClientPacketExecutor.handleRequest(msg));
        }
    }
}
