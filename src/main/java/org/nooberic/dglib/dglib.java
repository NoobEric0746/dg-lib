package org.nooberic.dglib;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.TickEvent;
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.client.DgKeyBindings;
import org.nooberic.dglib.client.StrengthControlScreen;
import org.nooberic.dglib.command.DgLibCommands;
import org.nooberic.dglib.multiplayer.DgServerCoyoteApi;
import org.nooberic.dglib.network.DgNetworking;
import org.nooberic.dglib.service.ConnectionState;
import org.nooberic.dglib.service.DeviceStatus;
import org.slf4j.Logger;

@Mod(dglib.MODID)
public class dglib {

    public static final String MODID = "dglib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public dglib() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(DgNetworking::register);
        LOGGER.info("DG Lib common setup complete");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DgLibCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            DgServerCoyoteApi.get().onPlayerLogout(serverPlayer);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                DgLibApi.get().initialize();
                registerBuiltInPulses();
                LOGGER.info("DG Lib client API initialized");
            });
        }

        private static void registerBuiltInPulses() {
            registerPulse("basic_breath", "basic_breath.frame");
            registerPulse("chaos", "chaos.frame");
            registerPulse("const", "const.frame");
        }

        private static void registerPulse(String id, String fileName) {
            boolean ok = DgLibApi.get().registerPulse(id, fileName);
            if (ok) {
                LOGGER.info("Pulse loaded: {} <- {}", id, fileName);
            } else {
                LOGGER.warn("Pulse load failed: {} <- {}", id, fileName);
            }
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(DgKeyBindings.OPEN_DG_UI);
            event.register(DgKeyBindings.EMERGENCY_STOP);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            DgLibApi.get().shutdown();
            LOGGER.info("DG Lib client API shutdown on logout");
        }

        @SubscribeEvent
        public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }
            if (mc.screen != null) {
                return;
            }

            int hudMode = Config.getHudDisplayMode();
            if (hudMode == 1) {
                return;
            }

            DeviceStatus status = DgLibApi.get().getStatus();
            ConnectionState state = DgLibApi.get().getConnectionState();
            String pairedText = DgLibApi.get().isPaired() ? "已配对" : "未配对(" + state + ")";
            String aCurrent = hudMode == 2 ? "??" : String.valueOf(status.getChannelAStrength());
            String bCurrent = hudMode == 2 ? "??" : String.valueOf(status.getChannelBStrength());

            GuiGraphics g = event.getGuiGraphics();
            int x = 8;
            int y = 8;
            g.drawString(mc.font, "DG-LAB: " + pairedText, x, y, DgLibApi.get().isPaired() ? 0x55FF55 : 0xFF5555);
            g.drawString(mc.font, "A: " + aCurrent + "/" + status.getChannelALimit(), x, y + 12, 0xFFFFFF);
            g.drawString(mc.font, "B: " + bCurrent + "/" + status.getChannelBLimit(), x, y + 24, 0xFFFFFF);
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }

            if (DgKeyBindings.EMERGENCY_STOP.consumeClick()) {
                DgLibApi.get().setStrength(1, 0);
                DgLibApi.get().setStrength(2, 0);
                DgLibApi.get().disconnect();
                mc.player.displayClientMessage(Component.literal("§c紧急停止: AB已置0并断开"), false);
                return;
            }

            if (mc.screen != null) {
                return;
            }

            if (DgKeyBindings.OPEN_DG_UI.consumeClick()) {
                mc.setScreen(new StrengthControlScreen());
            }
        }
    }
}
