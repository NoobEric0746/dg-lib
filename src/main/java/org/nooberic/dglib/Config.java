package org.nooberic.dglib;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = dglib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> WS_URL = BUILDER
        .comment("DG-LAB websocket endpoint. Recommended for mobile pairing: wss://ws.dungeon-lab.cn/ . For local mock backend use ws://<LAN-IP>:9999")
        .define("wsUrl", "wss://ws.dungeon-lab.cn/");

    private static final ForgeConfigSpec.BooleanValue AUTO_RECONNECT = BUILDER
        .comment("Reconnect automatically when socket is disconnected")
        .define("autoReconnect", true);

    private static final ForgeConfigSpec.IntValue RECONNECT_BASE_MS = BUILDER
        .comment("Base reconnect delay in milliseconds")
        .defineInRange("reconnectBaseMs", 1000, 200, 120000);

    private static final ForgeConfigSpec.IntValue RECONNECT_MAX_MS = BUILDER
        .comment("Maximum reconnect delay in milliseconds")
        .defineInRange("reconnectMaxMs", 15000, 1000, 300000);

    private static final ForgeConfigSpec.IntValue CONNECT_TIMEOUT_MS = BUILDER
        .comment("Connection timeout in milliseconds")
        .defineInRange("connectTimeoutMs", 8000, 1000, 120000);

    private static final ForgeConfigSpec.BooleanValue LOG_TRAFFIC = BUILDER
        .comment("Log inbound/outbound socket payloads")
        .define("logTraffic", false);

    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_CHANNEL = BUILDER
        .comment("Default channel for simple control command: A or B")
        .define("defaultChannel", "A", Config::validateChannel);

    private static final ForgeConfigSpec.IntValue PAIN_A = BUILDER
        .comment("Custom pain strength for channel A (0-200), used by addon modules")
        .defineInRange("painA", 0, 0, 200);

    private static final ForgeConfigSpec.IntValue PAIN_B = BUILDER
        .comment("Custom pain strength for channel B (0-200), used by addon modules")
        .defineInRange("painB", 0, 0, 200);

    private static final ForgeConfigSpec.IntValue FLOOR_A = BUILDER
        .comment("Custom sensation floor for channel A (0-200), used by addon modules")
        .defineInRange("sensationFloorA", 0, 0, 200);

    private static final ForgeConfigSpec.IntValue FLOOR_B = BUILDER
        .comment("Custom sensation floor for channel B (0-200), used by addon modules")
        .defineInRange("sensationFloorB", 0, 0, 200);

    private static final ForgeConfigSpec.IntValue HUD_DISPLAY_MODE = BUILDER
        .comment("Top-left HUD display mode: 0=normal, 1=hidden, 2=hide current strength only")
        .defineInRange("hudDisplayMode", 0, 0, 2);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String wsUrl;
    public static boolean autoReconnect;
    public static int reconnectBaseMs;
    public static int reconnectMaxMs;
    public static int connectTimeoutMs;
    public static boolean logTraffic;
    public static String defaultChannel;
    public static int painA;
    public static int painB;
    public static int sensationFloorA;
    public static int sensationFloorB;
    public static int hudDisplayMode;

    private static boolean validateChannel(final Object value) {
    if (!(value instanceof String channel)) {
        return false;
    }
    String normalized = channel.trim().toUpperCase();
    return "A".equals(normalized) || "B".equals(normalized);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    wsUrl = WS_URL.get().trim();
    autoReconnect = AUTO_RECONNECT.get();
    reconnectBaseMs = RECONNECT_BASE_MS.get();
    reconnectMaxMs = RECONNECT_MAX_MS.get();
    connectTimeoutMs = CONNECT_TIMEOUT_MS.get();
    logTraffic = LOG_TRAFFIC.get();
    defaultChannel = DEFAULT_CHANNEL.get().trim().toUpperCase();
    painA = PAIN_A.get();
    painB = PAIN_B.get();
    sensationFloorA = FLOOR_A.get();
    sensationFloorB = FLOOR_B.get();
    hudDisplayMode = HUD_DISPLAY_MODE.get();
    }

    public static int getPainStrength(int channel) {
        return channel == 2 ? painB : painA;
    }

    public static int getSensationFloor(int channel) {
        return channel == 2 ? sensationFloorB : sensationFloorA;
    }

    public static void setPainStrength(int channel, int value) {
        int clamped = clamp200(value);
        if (channel == 2) {
            painB = clamped;
            PAIN_B.set(clamped);
        } else {
            painA = clamped;
            PAIN_A.set(clamped);
        }
        SPEC.save();
    }

    public static void setSensationFloor(int channel, int value) {
        int clamped = clamp200(value);
        if (channel == 2) {
            sensationFloorB = clamped;
            FLOOR_B.set(clamped);
        } else {
            sensationFloorA = clamped;
            FLOOR_A.set(clamped);
        }
        SPEC.save();
    }

    public static int getHudDisplayMode() {
        return hudDisplayMode;
    }

    public static void setHudDisplayMode(int mode) {
        int clamped = Math.max(0, Math.min(2, mode));
        hudDisplayMode = clamped;
        HUD_DISPLAY_MODE.set(clamped);
        SPEC.save();
    }

    public static int cycleHudDisplayMode() {
        int next = (hudDisplayMode + 1) % 3;
        setHudDisplayMode(next);
        return next;
    }

    private static int clamp200(int value) {
        return Math.max(0, Math.min(200, value));
    }
}
