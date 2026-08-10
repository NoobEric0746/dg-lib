package org.nooberic.dglib.client.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientDisconnectNotifier {
    private ClientDisconnectNotifier() {
    }

    public static void onDeviceDisconnected() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§c设备已断开"));
            }
        });
    }
}
