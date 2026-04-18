package org.nooberic.dglib.pulse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Pulse {
    public enum Channel {
        A,
        B;

        public static Channel from(String text) {
            if (text == null) {
                return A;
            }
            return "B".equalsIgnoreCase(text.trim()) ? B : A;
        }
    }

    private final String name;
    private final Channel channel;
    private final int seconds;
    private final List<String> frames;

    public Pulse(String name, Channel channel, int seconds, List<String> frames) {
        this.name = name == null ? "" : name;
        this.channel = channel == null ? Channel.A : channel;
        this.seconds = Math.max(1, Math.min(10, seconds));
        this.frames = Collections.unmodifiableList(new ArrayList<>(frames == null ? List.of() : frames));
    }

    public String getName() {
        return name;
    }

    public Channel getChannel() {
        return channel;
    }

    public int getSeconds() {
        return seconds;
    }

    public List<String> getFrames() {
        return frames;
    }

    public String[] frameArray() {
        return frames.toArray(String[]::new);
    }

    public void validate() {
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("Pulse frames cannot be empty");
        }
        for (String frame : frames) {
            if (frame == null || !frame.matches("^[0-9A-Fa-f]{16}$")) {
                throw new IllegalArgumentException("Invalid pulse frame: " + frame + " (expected 16-hex chars)");
            }
        }
    }
}
