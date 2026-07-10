package org.nooberic.dglib.pulse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PulseFileParser {
    private PulseFileParser() {
    }

    public static RegisteredPulse parse(Path path, String id) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return parse(text, id);
    }

    public static RegisteredPulse parse(String content, String id) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Pulse file content is empty");
        }
        return parseFrameJson(content.trim(), id);
    }

    private static RegisteredPulse parseFrameJson(String jsonText, String id) {
        JsonElement root = JsonParser.parseString(jsonText);
        JsonArray framesArray;

        if (root.isJsonArray()) {
            framesArray = root.getAsJsonArray();
        } else {
            JsonObject obj = root.getAsJsonObject();
            framesArray = obj.getAsJsonArray("frames");
        }

        if (framesArray == null) {
            throw new IllegalArgumentException("Pulse JSON requires 'frames' array");
        }

        List<String> frames = new ArrayList<>();
        for (JsonElement element : framesArray) {
            frames.add(element.getAsString());
        }

        RegisteredPulse pulse = new RegisteredPulse(id, frames);
        pulse.validate();
        return pulse;
    }

}
