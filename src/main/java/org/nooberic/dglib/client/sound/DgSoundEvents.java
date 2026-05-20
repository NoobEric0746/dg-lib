package org.nooberic.dglib.client.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.nooberic.dglib.dglib;

public final class DgSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, dglib.MODID);

    public static final RegistryObject<SoundEvent> ELECTRIC_SHOCK = SOUND_EVENTS.register(
            "electric_shock",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(dglib.MODID, "electric_shock"))
    );

    private DgSoundEvents() {
    }
}
