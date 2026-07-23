package org.nooberic.dglib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DgS2CParticleShockSoundPacket(float volume, float pitch) {
    public static void encode(DgS2CParticleShockSoundPacket message, FriendlyByteBuf buffer) {
        buffer.writeFloat(message.volume);
        buffer.writeFloat(message.pitch);
    }

    public static DgS2CParticleShockSoundPacket decode(FriendlyByteBuf buffer) {
        return new DgS2CParticleShockSoundPacket(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(DgS2CParticleShockSoundPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> DgClientParticleShockSoundPlayer.play(message.volume, message.pitch)
        ));
        context.setPacketHandled(true);
    }
}