package org.nooberic.dglib.client.particle;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.nooberic.dglib.dglib;

public final class DgParticleTypes {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, dglib.MODID);

    public static final RegistryObject<SimpleParticleType> ELECTRICITY =
            PARTICLES.register("electricity", () -> new SimpleParticleType(true));

    private DgParticleTypes() {
    }
}