package org.nooberic.dglib.client.screen;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * Simple test screen to verify screen opening works
 */
public class TestScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();

    public TestScreen() {
        super(Component.literal("Test Screen"));
        LOGGER.info("[TestScreen] Constructor called");
    }

    @Override
    protected void init() {
        super.init();
        LOGGER.info("[TestScreen] init() called, size: {}x{}", this.width, this.height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        LOGGER.info("[TestScreen] render() called");
        guiGraphics.centeredText(this.font, "TEST SCREEN", this.width / 2, 20, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font, "If you see this, rendering works!", this.width / 2, 50, 0xFF00FF00);
        super.extractRenderState(guiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
