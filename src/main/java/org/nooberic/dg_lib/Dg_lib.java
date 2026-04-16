package org.nooberic.dg_lib;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
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
import org.nooberic.dg_lib.api.DgLibApi;
import org.nooberic.dg_lib.command.DgLibCommands;
import org.nooberic.dg_lib.multiplayer.DgServerCoyoteApi;
import org.nooberic.dg_lib.network.DgNetworking;
import org.nooberic.dg_lib.service.ConnectionState;
import org.nooberic.dg_lib.service.DeviceStatus;
import org.slf4j.Logger;

@Mod(Dg_lib.MODID)
public class Dg_lib {

    public static final String MODID = "dg_lib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Dg_lib() {
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
                LOGGER.info("DG Lib client API initialized");
            });
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

            DeviceStatus status = DgLibApi.get().getStatus();
            ConnectionState state = DgLibApi.get().getConnectionState();
            String pairedText = DgLibApi.get().isPaired() ? "已配对" : "未配对(" + state + ")";

            GuiGraphics g = event.getGuiGraphics();
            int x = 8;
            int y = 8;
            g.drawString(mc.font, "DG-LAB: " + pairedText, x, y, DgLibApi.get().isPaired() ? 0x55FF55 : 0xFF5555);
            g.drawString(mc.font, "A: " + status.getChannelAStrength() + "/" + status.getChannelALimit(), x, y + 12, 0xFFFFFF);
            g.drawString(mc.font, "B: " + status.getChannelBStrength() + "/" + status.getChannelBLimit(), x, y + 24, 0xFFFFFF);
        }
    }
}
