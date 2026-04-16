package org.nooberic.dg_lib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.nooberic.dg_lib.Config;
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
    private Button pairButton;
    private Button hudModeButton;

    public StrengthControlScreen() {
        super(Component.literal("DG-LAB 控制面板"));
    }

    @Override
    protected void init() {
        super.init();
        LOGGER.info("[StrengthControlScreen] Initializing strength control UI");

        DeviceStatus status = DgLibApi.get().getStatus();
        int columnWidth = 160;
        int sliderHeight = 20;
        int topY = 62;
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

        this.hudModeButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            Config.cycleHudDisplayMode();
            updateHudModeButtonText();
        }).pos(8, 8).width(120).build());

        this.addRenderableWidget(channelA.maxSlider);
        this.addRenderableWidget(channelA.currentSlider);
        this.addRenderableWidget(channelA.floorSlider);
        this.addRenderableWidget(channelA.painSlider);
        this.addRenderableWidget(channelB.maxSlider);
        this.addRenderableWidget(channelB.currentSlider);
        this.addRenderableWidget(channelB.floorSlider);
        this.addRenderableWidget(channelB.painSlider);

        this.pairButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> onPairButtonClicked())
            .pos(this.width / 2 - 65, this.height - 36)
            .width(130)
            .build());

        updatePairButtonText();
        updateHudModeButtonText();

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
        updatePairButtonText();
        updateHudModeButtonText();
        }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, "DG-LAB 控制面板", this.width / 2, 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "强度上限请在手机端修改", this.width / 2, 28, 0x888888);
        guiGraphics.drawCenteredString(this.font,
            "感受阈值是能感到电流刺激的最小强度,痛感阈值是感觉到痛的最小强度",
            this.width / 2, this.height - 62, 0x888888);
        guiGraphics.drawCenteredString(this.font,
            "模组可以读取这些值来实现更精准的强度控制(也可能没有这样做) : )",
            this.width / 2, this.height - 50, 0x888888);

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
            this.maxSlider = new ValueSlider(x, y, width, height, tag + "上限", maxValue, false, v -> {});
            this.currentSlider = new ValueSlider(x, y + spacing, width, height, tag + "当前强度", currentValue, true,
                    v -> DgLibApi.get().setStrength(channel, v));
            this.floorSlider = new ValueSlider(x, y + spacing * 2, width, height, tag + "感受阈值", floorValue, true,
                    v -> DgLibApi.get().setSensationLowerLimit(channel, v));
            this.painSlider = new ValueSlider(x, y + spacing * 3, width, height, tag + "痛感阈值", painValue, true,
                v -> DgLibApi.get().setPainStrength(channel, v));
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

    private void onPairButtonClicked() {
        if (DgLibApi.get().isPaired()) {
            DgLibApi.get().disconnect();
        } else {
            DgLibApi.get().connect();
            autoOpenQrUiAfterConnect();
        }
        updatePairButtonText();
    }

    private void updatePairButtonText() {
        if (pairButton == null) {
            return;
        }
        pairButton.setMessage(Component.literal(DgLibApi.get().isPaired() ? "取消配对" : "配对"));
    }

    private void updateHudModeButtonText() {
        if (hudModeButton == null) {
            return;
        }
        int mode = Config.getHudDisplayMode();
        String text = switch (mode) {
            case 1 -> "指示:隐藏";
            case 2 -> "指示:隐藏强度";
            default -> "指示:正常";
        };
        hudModeButton.setMessage(Component.literal(text));
    }

    private void autoOpenQrUiAfterConnect() {
        Thread worker = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                DeviceStatus status = DgLibApi.get().getStatus();
                String wsUrl = status.getWsUrl();
                if (wsUrl != null && !wsUrl.isEmpty() && (wsUrl.startsWith("ws://") || wsUrl.startsWith("wss://"))) {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.execute(() -> {
                        try {
                            minecraft.setScreen(new QrCodeScreen(wsUrl));
                        } catch (Throwable e) {
                            LOGGER.warn("[StrengthControlScreen] Failed to open QR UI", e);
                        }
                    });
                    return;
                }
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "dg-lib-ui-qr-open");
        worker.setDaemon(true);
        worker.start();
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
