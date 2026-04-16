package org.nooberic.dg_lib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.nooberic.dg_lib.api.DgLibApi;
import org.nooberic.dg_lib.service.DeviceStatus;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Strength Control Screen for DG-LAB channels
 * 用于控制两个通道强度上限的设置界面
 */
@OnlyIn(Dist.CLIENT)
public class StrengthControlScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StrengthSlider sliderChannelA;
    private StrengthSlider sliderChannelB;

    public StrengthControlScreen() {
        super(Component.literal("DG-LAB Strength Control"));
    }

    @Override
    protected void init() {
        super.init();
        LOGGER.info("[StrengthControlScreen] Initializing strength control UI");

        DeviceStatus status = DgLibApi.get().getStatus();
        int limitA = status.getChannelALimit();
        int limitB = status.getChannelBLimit();

        int centerX = this.width / 2;
        int sliderWidth = 150;

        // 通道 A 滑动条
        this.sliderChannelA = new StrengthSlider(
                centerX - sliderWidth / 2,
                this.height / 2 - 40,
                sliderWidth,
                20,
                "Channel A: ",
                limitA,
                1
        );
        this.addRenderableWidget(this.sliderChannelA);

        // 通道 B 滑动条
        this.sliderChannelB = new StrengthSlider(
                centerX - sliderWidth / 2,
                this.height / 2 + 10,
                sliderWidth,
                20,
                "Channel B: ",
                limitB,
                2
        );
        this.addRenderableWidget(this.sliderChannelB);

        // 确认按钮
        this.addRenderableWidget(
                this.addRenderableWidget(
                        net.minecraft.client.gui.components.Button.builder(
                                Component.literal("Close"),
                                button -> this.minecraft.setScreen(null)
                        ).pos(centerX - 50, this.height - 50).width(100).build()
                )
        );

        LOGGER.info("[StrengthControlScreen] Channel A limit: {}, Channel B limit: {}", limitA, limitB);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, "DG-LAB Strength Control", this.width / 2, 20, 0xFFFFFF);

        // 绘制当前值
        if (sliderChannelA != null) {
            guiGraphics.drawString(this.font, "A: " + sliderChannelA.currentValue + "/200",
                    (this.width - 150) / 2, this.height / 2 - 25, 0xAAAAAA);
        }
        if (sliderChannelB != null) {
            guiGraphics.drawString(this.font, "B: " + sliderChannelB.currentValue + "/200",
                    (this.width - 150) / 2, this.height / 2 + 25, 0xAAAAAA);
        }

        guiGraphics.drawCenteredString(this.font, "Press ESC to close", this.width / 2, this.height - 20, 0x666666);

        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) { // ESC
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }

    /**
     * 自定义滑动条类，用于控制强度上限
     */
    private static class StrengthSlider extends AbstractSliderButton {
        private final int channel;
        public int currentValue;

        public StrengthSlider(int x, int y, int width, int height, String prefix, int initialValue, int channel) {
            super(x, y, width, height, Component.literal(prefix + initialValue), (double) initialValue / 200.0);
            this.channel = channel;
            this.currentValue = initialValue;
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal("Channel " + (channel == 1 ? "A" : "B") + ": " + this.currentValue));
        }

        @Override
        protected void applyValue() {
            this.currentValue = (int) (this.value * 200);
            this.updateMessage();
            // 设置强度值
            DgLibApi.get().setStrength(channel, this.currentValue);
            LOGGER.info("[StrengthSlider] Channel {} set to {}", channel, this.currentValue);
        }
    }
}
