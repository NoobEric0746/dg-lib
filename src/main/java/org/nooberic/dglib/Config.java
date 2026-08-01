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
        .translation("config.dglib.ws_url")
        .define("wsUrl", "wss://ws.dungeon-lab.cn/");

    private static final ForgeConfigSpec.BooleanValue AUTO_RECONNECT = BUILDER
        .comment("Reconnect automatically when socket is disconnected")
        .translation("config.dglib.auto_reconnect")
        .define("autoReconnect", true);

    private static final ForgeConfigSpec.IntValue RECONNECT_BASE_MS = BUILDER
        .comment("Base reconnect delay in milliseconds")
        .translation("config.dglib.reconnect_base_ms")
        .defineInRange("reconnectBaseMs", 1000, 200, 120000);

    private static final ForgeConfigSpec.IntValue RECONNECT_MAX_MS = BUILDER
        .comment("Maximum reconnect delay in milliseconds")
        .translation("config.dglib.reconnect_max_ms")
        .defineInRange("reconnectMaxMs", 15000, 1000, 300000);

    private static final ForgeConfigSpec.IntValue CONNECT_TIMEOUT_MS = BUILDER
        .comment("Connection timeout in milliseconds")
        .translation("config.dglib.connect_timeout_ms")
        .defineInRange("connectTimeoutMs", 8000, 1000, 120000);

    private static final ForgeConfigSpec.IntValue PAIN_A = BUILDER
        .comment("Custom pain strength for channel A (0-200), used by addon modules")
        .translation("config.dglib.pain_a")
        .defineInRange("painA", 0, 0, 200);

    private static final ForgeConfigSpec.IntValue PAIN_B = BUILDER
        .comment("Custom pain strength for channel B (0-200), used by addon modules")
        .translation("config.dglib.pain_b")
        .defineInRange("painB", 0, 0, 200);

    private static final ForgeConfigSpec.IntValue FLOOR_A = BUILDER
        .comment("Custom sensation floor for channel A (0-200), used by addon modules")
        .translation("config.dglib.sensation_floor_a")
        .defineInRange("sensationFloorA", 0, 0, 200);

    private static final ForgeConfigSpec.IntValue FLOOR_B = BUILDER
        .comment("Custom sensation floor for channel B (0-200), used by addon modules")
        .translation("config.dglib.sensation_floor_b")
        .defineInRange("sensationFloorB", 0, 0, 200);

    private static final ForgeConfigSpec.IntValue HUD_DISPLAY_MODE = BUILDER
        .comment("Top-left HUD display mode: 0=normal, 1=hidden, 2=hide current strength only")
        .translation("config.dglib.hud_display_mode")
        .defineInRange("hudDisplayMode", 0, 0, 2);

    private static final ForgeConfigSpec.BooleanValue PARTICLE_SHOCK_SOUND_ENABLED = BUILDER
        .comment("Play the electric shock sound when electricity particles are spawned")
        .translation("config.dglib.particle_shock_sound_enabled")
        .define("particleShockSoundEnabled", true);

    private static final ForgeConfigSpec.DoubleValue PARTICLE_SHOCK_SOUND_VOLUME = BUILDER
        .comment("Base volume for the particle shock sound")
        .translation("config.dglib.particle_shock_sound_volume")
        .defineInRange("particleShockSoundVolume", 0.9D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue PARTICLE_SHOCK_SOUND_PITCH = BUILDER
        .comment("Pitch for the particle shock sound")
        .translation("config.dglib.particle_shock_sound_pitch")
        .defineInRange("particleShockSoundPitch", 1.0D, 0.5D, 2.0D);

    private static final ForgeConfigSpec.IntValue PARTICLE_SHOCK_SOUND_COOLDOWN_TICKS = BUILDER
        .comment("Minimum ticks between particle shock sounds for the same target player")
        .translation("config.dglib.particle_shock_sound_cooldown_ticks")
        .defineInRange("particleShockSoundCooldownTicks", 10, 0, 1200);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String wsUrl;
    public static boolean autoReconnect;
    public static int reconnectBaseMs;
    public static int reconnectMaxMs;
    public static int connectTimeoutMs;
    public static int painA;
    public static int painB;
    public static int sensationFloorA;
    public static int sensationFloorB;
    public static int hudDisplayMode;
    public static boolean particleShockSoundEnabled;
    public static float particleShockSoundVolume;
    public static float particleShockSoundPitch;
    public static int particleShockSoundCooldownTicks;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    wsUrl = WS_URL.get().trim();
    autoReconnect = AUTO_RECONNECT.get();
    reconnectBaseMs = RECONNECT_BASE_MS.get();
    reconnectMaxMs = RECONNECT_MAX_MS.get();
    connectTimeoutMs = CONNECT_TIMEOUT_MS.get();
    painA = PAIN_A.get();
    painB = PAIN_B.get();
    sensationFloorA = FLOOR_A.get();
    sensationFloorB = FLOOR_B.get();
    hudDisplayMode = HUD_DISPLAY_MODE.get();
    particleShockSoundEnabled = PARTICLE_SHOCK_SOUND_ENABLED.get();
    particleShockSoundVolume = PARTICLE_SHOCK_SOUND_VOLUME.get().floatValue();
    particleShockSoundPitch = PARTICLE_SHOCK_SOUND_PITCH.get().floatValue();
    particleShockSoundCooldownTicks = PARTICLE_SHOCK_SOUND_COOLDOWN_TICKS.get();
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
