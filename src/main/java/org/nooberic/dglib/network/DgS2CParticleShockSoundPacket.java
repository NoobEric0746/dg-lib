package org.nooberic.dglib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.nooberic.dglib.dglib;

public record DgS2CParticleShockSoundPacket(float volume, float pitch) implements CustomPacketPayload {
    public static final Type<DgS2CParticleShockSoundPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(dglib.MODID, "particle_shock_sound"));
    public static final StreamCodec<FriendlyByteBuf, DgS2CParticleShockSoundPacket> STREAM_CODEC =
            StreamCodec.of((buffer, message) -> encode(message, buffer), DgS2CParticleShockSoundPacket::decode);

    @Override
    public Type<DgS2CParticleShockSoundPacket> type() {
        return TYPE;
    }

    public static void encode(DgS2CParticleShockSoundPacket message, FriendlyByteBuf buffer) {
        buffer.writeFloat(message.volume);
        buffer.writeFloat(message.pitch);
    }

    public static DgS2CParticleShockSoundPacket decode(FriendlyByteBuf buffer) {
        return new DgS2CParticleShockSoundPacket(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(DgS2CParticleShockSoundPacket message, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> DgClientParticleShockSoundPlayer.play(message.volume, message.pitch));
        }
    }
}