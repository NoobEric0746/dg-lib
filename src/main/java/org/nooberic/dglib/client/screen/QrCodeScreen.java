package org.nooberic.dglib.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * QR Code Display Screen for DG-LAB pairing
 * 显示二维码的 GUI 屏幕，用于郊狼设备配对
 */
@OnlyIn(Dist.CLIENT)
public class QrCodeScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int QR_CODE_SIZE = 300;

    private final String wsUrl;
    private DynamicTexture qrTexture;
    private ResourceLocation qrTextureLocation;

    public QrCodeScreen(String wsUrl) {
        super(Component.translatable("screen.dglib.qr_code.title"));
        this.wsUrl = wsUrl;
    }

    @Override
    protected void init() {
        super.init();
        LOGGER.info("[QrCodeScreen] init() called, wsUrl: {}", wsUrl);

        try {
            generateQrCode();
            LOGGER.info("[QrCodeScreen] generateQrCode() completed successfully");
        } catch (Exception e) {
            LOGGER.error("[QrCodeScreen] Unexpected exception in generateQrCode()", e);
        }
    }

    private void generateQrCode() throws Exception {
        if (qrTextureLocation != null) {
            LOGGER.info("QR texture already generated");
            return;
        }

        String qrContent = "https://www.dungeon-lab.com/app-download.php#DGLAB-SOCKET#" + wsUrl;
        String encoded = URLEncoder.encode(qrContent, StandardCharsets.UTF_8);
        String requestUrl = "https://api.qrserver.com/v1/create-qr-code/?size=" + QR_CODE_SIZE + "x" + QR_CODE_SIZE + "&ecc=M&margin=2&data=" + encoded;

        LOGGER.info("[QrCodeScreen] Requesting QR image from API");
        try (InputStream in = URI.create(requestUrl).toURL().openStream()) {
            NativeImage image = NativeImage.read(in);
            qrTexture = new DynamicTexture(image);
            qrTextureLocation = Minecraft.getInstance().getTextureManager().register("dglib_qr_" + Integer.toHexString(wsUrl.hashCode()), qrTexture);
        }

        LOGGER.info("[QrCodeScreen] QR texture created: {}", qrTextureLocation);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        if (qrTextureLocation != null) {
            int drawSize = Math.min(Math.min(this.width - 60, this.height - 140), QR_CODE_SIZE);
            int qrX = (this.width - drawSize) / 2;
            int qrY = (this.height - drawSize) / 2 - 20;

            guiGraphics.fill(qrX - 8, qrY - 8, qrX + drawSize + 8, qrY + drawSize + 8, 0xFFFFFFFF);
            guiGraphics.blit(qrTextureLocation, qrX, qrY, 0, 0, drawSize, drawSize, drawSize, drawSize);

            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.dglib.qr_code.scan_prompt"), this.width / 2, qrY + drawSize + 20, 0xAAAAAA);
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.dglib.qr_code.browser_prompt"), this.width / 2, qrY + drawSize + 35, 0xAAAAAA);
            guiGraphics.drawCenteredString(this.font, "WebSocket: " + wsUrl, this.width / 2, qrY + drawSize + 55, 0x888888);
        } else {
            guiGraphics.drawCenteredString(this.font, "Generating QR code image...", this.width / 2, this.height / 2, 0xFFFFFF);
        }

        guiGraphics.drawCenteredString(this.font, "Press ESC to close", this.width / 2, this.height - 20, 0x666666);

    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) {
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        if (qrTexture != null) {
            qrTexture.close();
            qrTexture = null;
            qrTextureLocation = null;
        }
    }
}
