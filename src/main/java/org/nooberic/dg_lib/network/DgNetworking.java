package org.nooberic.dg_lib.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.nooberic.dg_lib.Dg_lib;

public final class DgNetworking {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Dg_lib.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean registered = false;

    private DgNetworking() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        int index = 0;
        CHANNEL.messageBuilder(DgS2CClientOpRequestPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DgS2CClientOpRequestPacket::encode)
                .decoder(DgS2CClientOpRequestPacket::decode)
                .consumerMainThread(DgS2CClientOpRequestPacket::handle)
                .add();

        CHANNEL.messageBuilder(DgC2SClientOpResponsePacket.class, index, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DgC2SClientOpResponsePacket::encode)
                .decoder(DgC2SClientOpResponsePacket::decode)
                .consumerMainThread(DgC2SClientOpResponsePacket::handle)
                .add();

        registered = true;
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
