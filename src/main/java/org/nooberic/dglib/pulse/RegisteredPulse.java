package org.nooberic.dglib.pulse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RegisteredPulse {
    private final String id;
    private final List<String> frames;

    public RegisteredPulse(String id, List<String> frames) {
        this.id = id == null ? "" : id;
        this.frames = Collections.unmodifiableList(new ArrayList<>(frames == null ? List.of() : frames));
    }

    public String getId() {
        return id;
    }

    public List<String> getFrames() {
        return frames;
    }

    public String[] frameArray() {
        return frames.toArray(String[]::new);
    }

    public void validate() {
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("Pulse id cannot be empty");
        }
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