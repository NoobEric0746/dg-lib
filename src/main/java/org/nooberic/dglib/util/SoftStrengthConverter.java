package org.nooberic.dglib.util;

import org.nooberic.dglib.Config;
import org.nooberic.dglib.api.DgLibApi;

public final class SoftStrengthConverter {
    private SoftStrengthConverter() {
    }

    public static int toRealStrength(int channel, int softStrength) {
        int normalizedChannel = channel == 2 ? 2 : 1;
        int clampedSoftStrength = Math.max(0, Math.min(100, softStrength));
        int sensationFloor = Config.getSensationFloor(normalizedChannel);
        int painStrength = Config.getPainStrength(normalizedChannel);
        int strengthLimit = DgLibApi.get().getStrengthLimit(normalizedChannel);
        int realLimit = Math.max(0, Math.min(200, strengthLimit > 0 ? strengthLimit : 200));
        int safeFloor = Math.max(0, Math.min(realLimit, sensationFloor));
        int safePain = Math.max(safeFloor, Math.min(realLimit, painStrength));

        if (clampedSoftStrength <= 33) {
            return interpolate(clampedSoftStrength, 0, 33, 0, safeFloor);
        }
        if (clampedSoftStrength <= 66) {
            return interpolate(clampedSoftStrength, 34, 66, safeFloor, safePain);
        }
        return interpolate(clampedSoftStrength, 67, 100, safePain, realLimit);
    }

    private static int interpolate(int value, int inputMin, int inputMax, int outputMin, int outputMax) {
        if (inputMax <= inputMin) {
            return outputMax;
        }
        double ratio = (double) (value - inputMin) / (double) (inputMax - inputMin);
        int mapped = (int) Math.round(outputMin + ratio * (outputMax - outputMin));
        return Math.max(Math.min(outputMax, outputMin), Math.min(Math.max(outputMax, outputMin), mapped));
    }
}