package org.nooberic.dglib.client.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.nooberic.dglib.dglib;

public final class DgParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, dglib.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRICITY =
            PARTICLES.register("electricity", () -> new SimpleParticleType(false) {
            });

    private DgParticleTypes() {
    }
}