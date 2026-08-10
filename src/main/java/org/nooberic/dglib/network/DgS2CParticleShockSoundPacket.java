package org.nooberic.dglib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.nooberic.dglib.dglib;

public record DgS2CParticleShockSoundPacket(float volume, float pitch) implements CustomPacketPayload {
    public static final Type<DgS2CParticleShockSoundPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dglib.MODID, "s2c_particle_shock_sound"));

    public static final StreamCodec<FriendlyByteBuf, DgS2CParticleShockSoundPacket> STREAM_CODEC = StreamCodec.of(
            DgS2CParticleShockSoundPacket::encode,
            DgS2CParticleShockSoundPacket::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(FriendlyByteBuf buffer, DgS2CParticleShockSoundPacket message) {
        buffer.writeFloat(message.volume);
        buffer.writeFloat(message.pitch);
    }

    public static DgS2CParticleShockSoundPacket decode(FriendlyByteBuf buffer) {
        return new DgS2CParticleShockSoundPacket(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(DgS2CParticleShockSoundPacket message, IPayloadContext context) {
        context.enqueueWork(() -> DgClientParticleShockSoundPlayer.play(message.volume, message.pitch));
    }
}
