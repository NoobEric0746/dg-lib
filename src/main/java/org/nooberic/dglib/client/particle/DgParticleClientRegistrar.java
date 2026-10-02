package org.nooberic.dglib.client.particle;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.nooberic.dglib.dglib;

@EventBusSubscriber(modid = dglib.MODID, value = Dist.CLIENT)
public final class DgParticleClientRegistrar {
    private DgParticleClientRegistrar() {
    }

    @SubscribeEvent
    public static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(DgParticleTypes.ELECTRICITY.get(), ElectricityParticle.Provider::new);
    }
}