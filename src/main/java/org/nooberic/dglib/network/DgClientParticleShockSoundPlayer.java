package org.nooberic.dglib.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.nooberic.dglib.dglib;

public final class DgClientParticleShockSoundPlayer {
    private DgClientParticleShockSoundPlayer() {
    }

    public static void play(float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        minecraft.getSoundManager().play(new SimpleSoundInstance(
                ResourceLocation.fromNamespaceAndPath(dglib.MODID, "electric_shock"),
                SoundSource.PLAYERS,
                volume,
                pitch,
                minecraft.level.random,
                false,
                0,
                SimpleSoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true
        ));
    }
}