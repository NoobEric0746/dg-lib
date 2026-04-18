package org.nooberic.dglib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DgS2CClientOpRequestPacket {
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

    public static void handle(DgS2CClientOpRequestPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DgClientPacketExecutor.handleRequest(msg)));
        ctx.setPacketHandled(true);
    }
}
