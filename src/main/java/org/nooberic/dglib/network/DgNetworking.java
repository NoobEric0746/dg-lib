package org.nooberic.dglib.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class DgNetworking {
    private static final String PROTOCOL_VERSION = "1";

    private DgNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(DgS2CClientOpRequestPacket.TYPE, DgS2CClientOpRequestPacket.STREAM_CODEC, DgS2CClientOpRequestPacket::handle);
        registrar.playToClient(DgS2CParticleShockSoundPacket.TYPE, DgS2CParticleShockSoundPacket.STREAM_CODEC, DgS2CParticleShockSoundPacket::handle);
        registrar.playToServer(DgC2SClientOpResponsePacket.TYPE, DgC2SClientOpResponsePacket.STREAM_CODEC, DgC2SClientOpResponsePacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        PacketDistributor.sendToPlayer(player, (CustomPacketPayload) packet);
    }

    public static void sendToServer(Object packet) {
        ClientPacketDistributor.sendToServer((CustomPacketPayload) packet);
    }
}
