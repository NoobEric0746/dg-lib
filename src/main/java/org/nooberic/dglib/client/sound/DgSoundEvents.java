package org.nooberic.dglib.client.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.nooberic.dglib.dglib;

public final class DgSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, dglib.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_SHOCK = SOUND_EVENTS.register(
            "electric_shock",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(dglib.MODID, "electric_shock"))
    );

    private DgSoundEvents() {
    }
}
