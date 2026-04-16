package org.nooberic.dg_lib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
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

    private ChannelSliders channelA;
    private ChannelSliders channelB;

    public StrengthControlScreen() {
        super(Component.literal("DG-LAB Strength Control"));
    }

    @Override
    protected void init() {
        super.init();
        LOGGER.info("[StrengthControlScreen] Initializing strength control UI");

        DeviceStatus status = DgLibApi.get().getStatus();
        int columnWidth = 160;
        int sliderHeight = 20;
        int topY = 58;
        int rowSpacing = 26;
        int gap = 24;
        int totalWidth = columnWidth * 2 + gap;
        int startX = (this.width - totalWidth) / 2;

        this.channelA = new ChannelSliders(1, "A", startX, topY, columnWidth, sliderHeight, rowSpacing,
            status.getChannelALimit(), status.getChannelAStrength(),
            DgLibApi.get().getPainStrength(1), DgLibApi.get().getSensationLowerLimit(1));
        this.channelB = new ChannelSliders(2, "B", startX + columnWidth + gap, topY, columnWidth, sliderHeight, rowSpacing,
            status.getChannelBLimit(), status.getChannelBStrength(),
            DgLibApi.get().getPainStrength(2), DgLibApi.get().getSensationLowerLimit(2));

        this.addRenderableWidget(channelA.maxSlider);
        this.addRenderableWidget(channelA.currentSlider);
        this.addRenderableWidget(channelA.painSlider);
        this.addRenderableWidget(channelA.floorSlider);
        this.addRenderableWidget(channelB.maxSlider);
        this.addRenderableWidget(channelB.currentSlider);
        this.addRenderableWidget(channelB.painSlider);
        this.addRenderableWidget(channelB.floorSlider);

        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.minecraft.setScreen(null))
            .pos(this.width / 2 - 50, this.height - 36)
            .width(100)
            .build());

        LOGGER.info("[StrengthControlScreen] Channel A/B sliders initialized");
    }

        @Override
        public void tick() {
        super.tick();
        DeviceStatus status = DgLibApi.get().getStatus();
        if (channelA != null) {
            channelA.setLiveValues(status.getChannelALimit(), status.getChannelAStrength());
        }
        if (channelB != null) {
            channelB.setLiveValues(status.getChannelBLimit(), status.getChannelBStrength());
        }
        }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, "DG-LAB Strength Control", this.width / 2, 18, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "A / B Channels", this.width / 2, 32, 0xAAAAAA);
        guiGraphics.drawCenteredString(this.font, "Rows: Max(read-only), Current(read-only), Pain, Sensation Floor", this.width / 2, 44, 0x777777);

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

    private static final class ChannelSliders {
        private final int channel;
        private final String tag;
        private final ValueSlider maxSlider;
        private final ValueSlider currentSlider;
        private final ValueSlider painSlider;
        private final ValueSlider floorSlider;

        private ChannelSliders(int channel, String tag, int x, int y, int width, int height, int spacing,
                               int maxValue, int currentValue, int painValue, int floorValue) {
            this.channel = channel;
            this.tag = tag;
            this.maxSlider = new ValueSlider(x, y, width, height, tag + " Max", maxValue, false, v -> {});
                this.currentSlider = new ValueSlider(x, y + spacing, width, height, tag + " Current", currentValue, true,
                    v -> DgLibApi.get().setStrength(channel, v));
            this.painSlider = new ValueSlider(x, y + spacing * 2, width, height, tag + " Pain", painValue, true,
                    v -> DgLibApi.get().setPainStrength(channel, v));
            this.floorSlider = new ValueSlider(x, y + spacing * 3, width, height, tag + " Floor", floorValue, true,
                    v -> DgLibApi.get().setSensationLowerLimit(channel, v));
        }

        private void setLiveValues(int max, int current) {
            maxSlider.setRawValue(max);
            currentSlider.setRawValue(current);
            painSlider.setRawValue(DgLibApi.get().getPainStrength(channel));
            floorSlider.setRawValue(DgLibApi.get().getSensationLowerLimit(channel));
        }
    }

    private interface OnChange {
        void onChange(int value);
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final String label;
        private final boolean editable;
        private final OnChange onChange;
        private int currentValue;

        private ValueSlider(int x, int y, int width, int height, String label, int initialValue, boolean editable, OnChange onChange) {
            super(x, y, width, height, Component.empty(), toUnit(initialValue));
            this.label = label;
            this.editable = editable;
            this.onChange = onChange;
            this.active = editable;
            this.currentValue = clamp(initialValue);
            updateMessage();
        }

        private static double toUnit(int value) {
            return clamp(value) / 200.0;
        }

        private static int clamp(int value) {
            return Math.max(0, Math.min(200, value));
        }

        private void setRawValue(int value) {
            this.currentValue = clamp(value);
            this.value = toUnit(this.currentValue);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(label + ": " + currentValue));
        }

        @Override
        protected void applyValue() {
            this.currentValue = (int) Math.round(this.value * 200.0);
            updateMessage();
            if (editable) {
                onChange.onChange(currentValue);
            }
        }
    }
}
