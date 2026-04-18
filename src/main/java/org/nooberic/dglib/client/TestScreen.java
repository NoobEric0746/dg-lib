package org.nooberic.dglib.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Simple test screen to verify screen opening works
 */
@OnlyIn(Dist.CLIENT)
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
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        LOGGER.info("[TestScreen] render() called");
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, "TEST SCREEN", this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "If you see this, rendering works!", this.width / 2, 50, 0x00FF00);
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
