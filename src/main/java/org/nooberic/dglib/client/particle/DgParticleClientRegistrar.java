package org.nooberic.dglib.client.particle;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.nooberic.dglib.dglib;

@Mod.EventBusSubscriber(modid = dglib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DgParticleClientRegistrar {
    private DgParticleClientRegistrar() {
    }

    @SubscribeEvent
    public static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(DgParticleTypes.ELECTRICITY.get(), ElectricityParticle.Provider::new);
    }
}