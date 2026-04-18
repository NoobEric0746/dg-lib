package org.nooberic.dglib.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

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
    
    private final String wsUrl;
    private final String title;
    private DynamicTexture qrTexture;
    private ResourceLocation qrTextureLocation;
    private static final int QR_CODE_SIZE = 300;
    
    public QrCodeScreen(String wsUrl) {
        super(Component.literal("DG-LAB Pairing QR Code"));
        this.wsUrl = wsUrl;
        this.title = "DG-LAB Device Pairing";
    }
    
    @Override
    protected void init() {
        super.init();
        LOGGER.info("[QrCodeScreen] ✅ init() called, wsUrl: {}", wsUrl);

        try {
            generateQrCode();
            LOGGER.info("[QrCodeScreen] ✅ generateQrCode() completed successfully");
        } catch (Exception e) {
            LOGGER.error("[QrCodeScreen] ❌ Unexpected exception in generateQrCode()", e);
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

        LOGGER.info("[QrCodeScreen] ✅ QR texture created: {}", qrTextureLocation);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(guiGraphics);
        
        // 绘制标题
        guiGraphics.drawCenteredString(this.font, Component.literal(title), 
                this.width / 2, 20, 0xFFFFFF);
        
        if (qrTextureLocation != null) {
            int drawSize = Math.min(Math.min(this.width - 60, this.height - 140), QR_CODE_SIZE);
            int qrX = (this.width - drawSize) / 2;
            int qrY = (this.height - drawSize) / 2 - 20;

            guiGraphics.fill(qrX - 8, qrY - 8, qrX + drawSize + 8, qrY + drawSize + 8, 0xFFFFFFFF);
            guiGraphics.blit(qrTextureLocation, qrX, qrY, 0, 0, drawSize, drawSize, drawSize, drawSize);
            
            // 绘制提示文本
            String hint1 = "Scan this QR code with DG-LAB app";
            String hint2 = "or access the URL in your browser";
            guiGraphics.drawCenteredString(this.font, hint1, 
                this.width / 2, qrY + drawSize + 20, 0xAAAAAA);
            guiGraphics.drawCenteredString(this.font, hint2, 
                this.width / 2, qrY + drawSize + 35, 0xAAAAAA);
            
            // 绘制 URL
            String urlDisplay = "WebSocket: " + wsUrl;
            guiGraphics.drawCenteredString(this.font, urlDisplay, 
                this.width / 2, qrY + drawSize + 55, 0x888888);
        } else {
            guiGraphics.drawCenteredString(this.font, "Generating QR code image...",
                    this.width / 2, this.height / 2, 0xFFFFFF);
        }
        
        // 绘制返回提示
        guiGraphics.drawCenteredString(this.font, "Press ESC to close", 
                this.width / 2, this.height - 30, 0x666666);
        
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
    }
    
    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) { // ESC key
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
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
