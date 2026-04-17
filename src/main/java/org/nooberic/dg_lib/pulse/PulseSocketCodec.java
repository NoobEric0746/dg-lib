package org.nooberic.dg_lib.pulse;

import java.util.Locale;

public final class PulseSocketCodec {
    private PulseSocketCodec() {
    }

    public static PulseSocketData toSocketData(Pulse pulse) {
        if (pulse == null) {
            throw new IllegalArgumentException("Pulse cannot be null");
        }
        pulse.validate();

        String channel = pulse.getChannel() == Pulse.Channel.B ? "B" : "A";
        String[] normalizedFrames = pulse.getFrames().stream()
                .map(f -> f.toUpperCase(Locale.ROOT))
                .toArray(String[]::new);

        return new PulseSocketData(channel, pulse.getSeconds(), normalizedFrames);
    }
}
