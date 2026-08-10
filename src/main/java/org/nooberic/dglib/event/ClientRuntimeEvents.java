package org.nooberic.dglib.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.client.screen.StrengthControlScreen;
import org.nooberic.dglib.client.screen.StrengthHudRenderer;
import org.nooberic.dglib.client.key.DgKeyBindings;
import org.nooberic.dglib.command.DgClientCommands;
import org.nooberic.dglib.dglib;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@EventBusSubscriber(modid = dglib.MODID, value = Dist.CLIENT)
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

@EventBusSubscriber(modid = dglib.MODID, value = Dist.CLIENT)
final class ClientForgeRuntimeEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientForgeRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        DgClientCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        DgLibApi.get().shutdown();
        LOGGER.info("DG Lib client API shutdown on logout");
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Post event) {
        StrengthHudRenderer.render(new StrengthHudRenderer.RenderContext(event.getGuiGraphics()));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (DgKeyBindings.EMERGENCY_STOP.consumeClick()) {
            DgLibApi.get().setStrength(1, 0);
            DgLibApi.get().setStrength(2, 0);
            DgLibApi.get().disconnect();
            mc.player.sendSystemMessage(Component.literal("Emergency stop: AB set to 0 and disconnected"));
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
