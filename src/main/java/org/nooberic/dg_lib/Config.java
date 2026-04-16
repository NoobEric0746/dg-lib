package org.nooberic.dg_lib;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Dg_lib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
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

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String wsUrl;
    public static boolean autoReconnect;
    public static int reconnectBaseMs;
    public static int reconnectMaxMs;
    public static int connectTimeoutMs;
    public static boolean logTraffic;
    public static String defaultChannel;

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
    }
}
