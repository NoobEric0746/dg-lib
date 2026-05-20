package org.nooberic.dglib.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import org.nooberic.dglib.client.particle.DgParticleTypes;

public final class ElectricityParticleUtil {
    private static final int DEFAULT_PARTICLE_COUNT = 32;
    private static final double SURFACE_EPSILON = 0.02D;

    private ElectricityParticleUtil() {
    }

    public static void spawnAroundPlayer(ServerPlayer player) {
        spawnAroundPlayer(player, DEFAULT_PARTICLE_COUNT);
    }

    public static void spawnAroundPlayer(ServerPlayer player, int particleCount) {
        if (player == null || particleCount <= 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
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