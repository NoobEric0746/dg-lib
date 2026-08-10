package org.nooberic.dglib.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class ElectricityParticle extends SimpleAnimatedParticle {
    /** Equivalent of the removed LightTexture.FULL_BRIGHT (0xF000F0). */
    private static final int FULL_BRIGHT = 15728880;
    private static final int MIN_LIFETIME = 6;
    private static final int MAX_LIFETIME = 6;
    private static final float MIN_SIZE = 0.125F;
    private static final float MAX_SIZE = 0.125F;
    private static final int SHRINK_START_TICK = 2;
    private static final int SHRINK_END_TICK = 6;
    private final float baseSize;

    protected ElectricityParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites, 0.0F);
        this.lifetime = MIN_LIFETIME + this.random.nextInt(MAX_LIFETIME - MIN_LIFETIME + 1);
        this.baseSize = MIN_SIZE + this.random.nextFloat() * (MAX_SIZE - MIN_SIZE);
        this.quadSize = this.baseSize;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSprite(this.sprites.get(this.age % 4, 4));
            if (this.age >= SHRINK_START_TICK) {
                float progress = (float) (this.age - SHRINK_START_TICK) / (float) (SHRINK_END_TICK - SHRINK_START_TICK);
                float clamped = Math.max(0.0F, Math.min(1.0F, progress));
                this.quadSize = this.baseSize * (1.0F - clamped);
            }
        }
    }

    @Override
    public int getLightCoords(float partialTick) {
        return FULL_BRIGHT;
    }

        public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd, RandomSource random) {
            return new ElectricityParticle(level, x, y, z, sprites);
        }
    }
}