package org.nooberic.dg_lib.pulse;

import java.util.ArrayList;
import java.util.List;

public final class PulseFrameResampler {
    private PulseFrameResampler() {
    }

    public static List<String> resize(List<String> frames, int targetCount) {
        if (frames == null || frames.isEmpty() || targetCount <= 0) {
            return List.of();
        }

        int sourceCount = frames.size();
        if (sourceCount == targetCount) {
            return new ArrayList<>(frames);
        }

        List<String> out = new ArrayList<>(targetCount);
        for (int i = 0; i < targetCount; i++) {
            int sourceIndex = (int) Math.floor(i * sourceCount / (double) targetCount);
            sourceIndex = Math.max(0, Math.min(sourceCount - 1, sourceIndex));
            out.add(frames.get(sourceIndex));
        }
        return out;
    }
}
