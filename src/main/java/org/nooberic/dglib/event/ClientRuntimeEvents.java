package org.nooberic.dglib.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.client.key.DgKeyBindings;
import org.nooberic.dglib.client.screen.StrengthControlScreen;
import org.nooberic.dglib.client.screen.StrengthHudRenderer;
import org.nooberic.dglib.dglib;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod.EventBusSubscriber(modid = dglib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRuntimeEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DgKeyBindings.OPEN_DG_UI);
        event.register(DgKeyBindings.EMERGENCY_STOP);
    }

}

@Mod.EventBusSubscriber(modid = dglib.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
final class ClientForgeRuntimeEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientForgeRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        DgLibApi.get().shutdown();
        LOGGER.info("DG Lib client API shutdown on logout");
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        StrengthHudRenderer.render(new StrengthHudRenderer.RenderContext(event.getGuiGraphics()));
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
