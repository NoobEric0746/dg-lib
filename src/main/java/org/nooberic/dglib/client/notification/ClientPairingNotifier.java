package org.nooberic.dglib.client.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.nooberic.dglib.client.screen.QrCodeScreen;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientPairingNotifier {
    private static final AtomicBoolean SUCCESS_SHOWN = new AtomicBoolean(false);

    private ClientPairingNotifier() {
    }

    public static void onPairedSuccess() {
        if (!SUCCESS_SHOWN.compareAndSet(false, true)) {
            return;
        }

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

    public static void reset() {
        SUCCESS_SHOWN.set(false);
    }
}
