package org.nooberic.dglib.client.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.nooberic.dglib.client.screen.QrCodeScreen;

public final class ClientPairingNotifier {
    private ClientPairingNotifier() {
    }

    public static void onPairedSuccess() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.screen instanceof QrCodeScreen) {
                minecraft.setScreen(null);
            }
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("§a连接成功"), false);
            }
        });
    }
}
