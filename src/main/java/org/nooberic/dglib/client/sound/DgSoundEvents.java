package org.nooberic.dglib.client.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.nooberic.dglib.dglib;

public final class DgSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, dglib.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_SHOCK = SOUND_EVENTS.register(
            "electric_shock",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(dglib.MODID, "electric_shock"))
    );

    private DgSoundEvents() {
    }
}
