package org.nooberic.dglib.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import org.nooberic.dglib.Config;
import org.nooberic.dglib.client.particle.DgParticleTypes;
import org.nooberic.dglib.network.DgNetworking;
import org.nooberic.dglib.network.DgS2CParticleShockSoundPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ElectricityParticleUtil {
    private static final int DEFAULT_PARTICLE_COUNT = 32;
    private static final double SURFACE_EPSILON = 0.02D;
    private static final double FULL_VOLUME_RADIUS = 5.0D;
    private static final double MAX_AUDIBLE_RADIUS = 8.0D;
    private static final double MAX_AUDIBLE_RADIUS_SQUARED = MAX_AUDIBLE_RADIUS * MAX_AUDIBLE_RADIUS;
    private static final Map<UUID, Long> LAST_SHOCK_SOUND_TICKS = new HashMap<>();

    private ElectricityParticleUtil() {
    }

    public static void spawnAroundPlayer(ServerPlayer player) {
        spawnAroundPlayer(player, DEFAULT_PARTICLE_COUNT);
    }

    public static void spawnAroundPlayer(ServerPlayer player, int particleCount) {
        if (player == null || particleCount <= 0) {
            return;
        }

        ServerLevel level = player.level();
        AABB box = player.getBoundingBox();
        RandomSource random = player.getRandom();

        for (int i = 0; i < particleCount; i++) {
            SurfacePoint point = randomSurfacePoint(box, random);
            level.sendParticles(
                    DgParticleTypes.ELECTRICITY.get(),
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }

        playShockSound(player);
    }

    private static void playShockSound(ServerPlayer target) {
        if (!Config.particleShockSoundEnabled) {
            return;
        }

        long gameTime = target.level().getGameTime();
        UUID targetId = target.getUUID();
        Long lastSoundTick = LAST_SHOCK_SOUND_TICKS.get(targetId);
        if (lastSoundTick != null && gameTime - lastSoundTick < Config.particleShockSoundCooldownTicks) {
            return;
        }
        LAST_SHOCK_SOUND_TICKS.put(targetId, gameTime);

        for (ServerPlayer listener : target.level().players()) {
            double distanceSquared = listener.distanceToSqr(target);
            if (distanceSquared >= MAX_AUDIBLE_RADIUS_SQUARED) {
                continue;
            }

            double distance = Math.sqrt(distanceSquared);
            float volumeMultiplier = distance <= FULL_VOLUME_RADIUS
                    ? 1.0F
                    : (float) ((MAX_AUDIBLE_RADIUS - distance) / (MAX_AUDIBLE_RADIUS - FULL_VOLUME_RADIUS));
            DgNetworking.sendToPlayer(
                    listener,
                    new DgS2CParticleShockSoundPacket(
                            Config.particleShockSoundVolume * volumeMultiplier,
                            Config.particleShockSoundPitch
                    )
            );
        }
    }

    private static SurfacePoint randomSurfacePoint(AABB box, RandomSource random) {
        double minX = box.minX - SURFACE_EPSILON;
        double maxX = box.maxX + SURFACE_EPSILON;
        double minY = box.minY;
        double maxY = box.maxY;
        double minZ = box.minZ - SURFACE_EPSILON;
        double maxZ = box.maxZ + SURFACE_EPSILON;

        int face = random.nextInt(6);
        double x;
        double y;
        double z;

        switch (face) {
            case 0 -> {
                x = minX;
                y = lerp(random, minY, maxY);
                z = lerp(random, minZ, maxZ);
            }
            case 1 -> {
                x = maxX;
                y = lerp(random, minY, maxY);
                z = lerp(random, minZ, maxZ);
            }
            case 2 -> {
                x = lerp(random, minX, maxX);
                y = minY;
                z = lerp(random, minZ, maxZ);
            }
            case 3 -> {
                x = lerp(random, minX, maxX);
                y = maxY;
                z = lerp(random, minZ, maxZ);
            }
            case 4 -> {
                x = lerp(random, minX, maxX);
                y = lerp(random, minY, maxY);
                z = minZ;
            }
            default -> {
                x = lerp(random, minX, maxX);
                y = lerp(random, minY, maxY);
                z = maxZ;
            }
        }

        return new SurfacePoint(x, y, z);
    }

    private static double lerp(RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private record SurfacePoint(double x, double y, double z) {
    }
}