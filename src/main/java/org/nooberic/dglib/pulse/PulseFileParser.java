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

    public static Pulse parse(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return parse(text);
    }

    public static Pulse parse(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Pulse file content is empty");
        }
        return parseFrameJson(content.trim());
    }

    private static Pulse parseFrameJson(String jsonText) {
        JsonObject obj = JsonParser.parseString(jsonText).getAsJsonObject();

        String name = readString(obj, "name", "");
        Pulse.Channel channel = Pulse.Channel.from(readString(obj, "channel", "A"));
        int seconds = readInt(obj, "seconds", 5);
        JsonArray framesArray = obj.getAsJsonArray("frames");

        if (framesArray == null) {
            throw new IllegalArgumentException("Pulse JSON requires 'frames' array");
        }

        List<String> frames = new ArrayList<>();
        for (JsonElement element : framesArray) {
            frames.add(element.getAsString());
        }

        Pulse pulse = new Pulse(name, channel, seconds, frames);
        pulse.validate();
        return pulse;
    }

    private static String readString(JsonObject obj, String key, String fallback) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        return element.getAsString();
    }

    private static int readInt(JsonObject obj, String key, int fallback) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

}
