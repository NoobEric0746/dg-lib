package org.nooberic.dglib.client.particle;

import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.nooberic.dglib.dglib;

public final class DgParticleTypes {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, dglib.MODID);

    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> ELECTRICITY =
            PARTICLES.register("electricity", () -> new SimpleParticleType(true));

    private DgParticleTypes() {
    }
}