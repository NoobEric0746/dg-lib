package org.nooberic.dglib.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.nooberic.dglib.Config;
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.service.ConnectionState;
import org.nooberic.dglib.service.DeviceStatus;

public final class StrengthHudRenderer {
    private StrengthHudRenderer() {
    }

    public static void render(RenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.screen != null) {
            return;
        }

        int hudMode = Config.getHudDisplayMode();
        if (hudMode == 1) {
            return;
        }

        DeviceStatus status = DgLibApi.get().getStatus();
        ConnectionState state = DgLibApi.get().getConnectionState();
        String pairedText = DgLibApi.get().isPaired() ? "已配对" : "未配对(" + state + ")";
        String aCurrent = hudMode == 2 ? "??" : String.valueOf(status.getChannelAStrength());
        String bCurrent = hudMode == 2 ? "??" : String.valueOf(status.getChannelBStrength());

        GuiGraphics g = context.guiGraphics();
        int x = 8;
        int y = 8;
        g.drawString(mc.font, "DG-LAB: " + pairedText, x, y, DgLibApi.get().isPaired() ? 0x55FF55 : 0xFF5555);
        g.drawString(mc.font, "A: " + aCurrent + "/" + status.getChannelALimit(), x, y + 12, 0xFFFFFF);
        g.drawString(mc.font, "B: " + bCurrent + "/" + status.getChannelBLimit(), x, y + 24, 0xFFFFFF);
    }

    public record RenderContext(GuiGraphics guiGraphics) {
    }
}
