package org.nooberic.dglib.event;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.dglib;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@EventBusSubscriber(modid = dglib.MODID)
public final class PulseRegistrationEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PulseRegistrationEvents() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            registerBuiltInPulses();
            LOGGER.info("DG Lib built-in pulses initialized");
        });
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            DgLibApi.get().initialize();
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
}
