package org.nooberic.dglib.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.nooberic.dglib.dglib;
import org.slf4j.Logger;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.nooberic.dglib.util.QrCodeGenerator;

/**
 * QR Code Display Screen for DG-LAB pairing
 * 显示二维码的 GUI 屏幕，用于配对设备
 */
public class QrCodeScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int QR_CODE_SIZE = 300;

    private final String wsUrl;
    private DynamicTexture qrTexture;
    private Identifier qrTextureLocation;

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

        String qrContent = QrCodeGenerator.generateQrContent(wsUrl);
        LOGGER.info("[QrCodeScreen] Generating QR locally, content length: {}", qrContent.length());

        BitMatrix matrix = new QRCodeWriter().encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);
        int size = matrix.getWidth();
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, size, size, true);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // black/white are symmetric under RGBA/ABGR packing, so setPixel is safe here
                image.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }

        String textureName = "dglib_qr_" + Integer.toHexString(wsUrl.hashCode());
        qrTexture = new DynamicTexture(() -> textureName, image);
        qrTextureLocation = Identifier.fromNamespaceAndPath(dglib.MODID, "qr_" + Integer.toHexString(wsUrl.hashCode()));
        Minecraft.getInstance().getTextureManager().register(qrTextureLocation, qrTexture);

        LOGGER.info("[QrCodeScreen] QR texture created: {}", qrTextureLocation);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {

        guiGraphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        if (qrTextureLocation != null) {
            int drawSize = Math.min(Math.min(this.width - 60, this.height - 140), QR_CODE_SIZE);
            int qrX = (this.width - drawSize) / 2;
            int qrY = (this.height - drawSize) / 2 - 20;

            guiGraphics.fill(qrX - 8, qrY - 8, qrX + drawSize + 8, qrY + drawSize + 8, 0xFFFFFFFF);
            // 26.1.2 signature: blit(location, x0, y0, x1, y1, u0, u1, v0, v1) with absolute coords and u0<u1, v0<v1
            guiGraphics.blit(qrTextureLocation, qrX, qrY, qrX + drawSize, qrY + drawSize, 0.0F, 1.0F, 0.0F, 1.0F);

            guiGraphics.centeredText(this.font, Component.translatable("screen.dglib.qr_code.scan_prompt"), this.width / 2, qrY + drawSize + 20, 0xFFAAAAAA);
            guiGraphics.centeredText(this.font, Component.translatable("screen.dglib.qr_code.browser_prompt"), this.width / 2, qrY + drawSize + 35, 0xFFAAAAAA);
            guiGraphics.centeredText(this.font, "WebSocket: " + wsUrl, this.width / 2, qrY + drawSize + 55, 0xFF888888);
        } else {
            guiGraphics.centeredText(this.font, "Generating QR code image...", this.width / 2, this.height / 2, 0xFFFFFFFF);
        }

        guiGraphics.centeredText(this.font, "Press ESC to close", this.width / 2, this.height - 20, 0xFF666666);

        super.extractRenderState(guiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(event);
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
