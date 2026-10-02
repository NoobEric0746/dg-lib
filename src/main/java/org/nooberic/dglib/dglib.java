package org.nooberic.dglib;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.nooberic.dglib.command.DgLibCommands;
import org.nooberic.dglib.client.particle.DgParticleTypes;
import org.nooberic.dglib.client.sound.DgSoundEvents;
import org.nooberic.dglib.multiplayer.DgServerCoyoteApi;
import org.nooberic.dglib.network.DgNetworking;
import org.slf4j.Logger;

@Mod(dglib.MODID)
public class dglib {

    public static final String MODID = "dglib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public dglib(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(DgNetworking::register);
        DgParticleTypes.PARTICLES.register(modEventBus);
        DgSoundEvents.SOUND_EVENTS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
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

}
