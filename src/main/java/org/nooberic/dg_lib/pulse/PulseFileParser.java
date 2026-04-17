package org.nooberic.dg_lib.pulse;

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
import java.util.Locale;

public final class PulseFileParser {
    private static final int SOCKET_MAX_FRAMES = 100;

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

        String text = content.trim();
        if (text.startsWith("Dungeonlab+pulse:")) {
            return parseDungeonLab(text);
        }
        if (text.startsWith("{")) {
            return parseJson(text);
        }
        return parseKeyValue(text);
    }

    private static Pulse parseDungeonLab(String text) {
        String body = text.substring("Dungeonlab+pulse:".length()).trim();
        String[] headerAndPayload = body.split("=", 2);

        List<String> frames = new ArrayList<>();
        if (headerAndPayload.length == 2) {
            String payload = headerAndPayload[1];
            for (String section : payload.split("\\+section\\+")) {
                String[] metaAndPoints = section.split("/", 2);
                if (metaAndPoints.length != 2) {
                    continue;
                }
                DungeonSectionMeta meta = parseSectionMeta(metaAndPoints[0]);
                String points = metaAndPoints[1];

                List<Integer> levels = parseDungeonLabLevels(points);
                if (levels.isEmpty()) {
                    continue;
                }

                int targetFrames = meta.frameCount > 0 ? meta.frameCount : levels.size();
                appendResampledFrames(frames, levels, targetFrames, meta);
            }
        }

        if (frames.size() > SOCKET_MAX_FRAMES) {
            frames = PulseFrameResampler.resize(frames, SOCKET_MAX_FRAMES);
        }

        int seconds = Math.max(1, Math.min(10, (int) Math.ceil(frames.size() / 10.0)));

        Pulse pulse = new Pulse("dungeonlab", Pulse.Channel.A, seconds, frames);
        pulse.validate();
        return pulse;
    }

    private static List<Integer> parseDungeonLabLevels(String points) {
        List<Integer> levels = new ArrayList<>();
        for (String token : points.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) {
                continue;
            }

            int dash = t.indexOf('-');
            String percentText = dash >= 0 ? t.substring(0, dash).trim() : t;
            float percent;
            try {
                percent = Float.parseFloat(percentText);
            } catch (NumberFormatException ignored) {
                continue;
            }
            int level = Math.max(0, Math.min(100, Math.round(percent)));
            levels.add(level);
        }
        return levels;
    }

    private static void appendResampledFrames(List<String> frames, List<Integer> levels, int targetFrames, DungeonSectionMeta meta) {
        if (targetFrames <= 0 || levels.isEmpty()) {
            return;
        }

        if (levels.size() == 1) {
            for (int i = 0; i < targetFrames; i++) {
                int frequency = frequencyAt(meta, i, targetFrames);
                String frame = toDgFrame(frequency, levels.get(0));
                frames.add(frame);
            }
            return;
        }

        for (int i = 0; i < targetFrames; i++) {
            int sourceIndex = (int) Math.floor(i * levels.size() / (double) targetFrames);
            int level = levels.get(Math.min(levels.size() - 1, Math.max(0, sourceIndex)));
            int frequency = frequencyAt(meta, i, targetFrames);
            frames.add(toDgFrame(frequency, level));
        }
    }

    private static int frequencyAt(DungeonSectionMeta meta, int index, int frameCount) {
        int start = clampWaveFrequency(meta.frequencyStart);
        int end = clampWaveFrequency(meta.frequencyEnd);
        if (meta.mode <= 1 || frameCount <= 1) {
            return start;
        }

        float phase = (index / (float) Math.max(1, frameCount - 1)) * meta.mode;
        float local = phase - (float) Math.floor(phase);
        return clampWaveFrequency(Math.round(start * (1f - local) + end * local));
    }

    private static String toDgFrame(int frequency, int level) {
        int f = clampWaveFrequency(frequency);
        int normalized = Math.max(0, Math.min(100, level));
        String fHex = String.format(Locale.ROOT, "%02X", f);
        String hex = String.format(Locale.ROOT, "%02X", normalized);
        return fHex.repeat(4) + hex.repeat(4);
    }

    private static int clampWaveFrequency(int value) {
        return Math.max(10, Math.min(240, value));
    }

    private static DungeonSectionMeta parseSectionMeta(String metaText) {
        String[] parts = metaText == null ? new String[0] : metaText.split(",");
        int freqStart = parts.length >= 1 ? parseIntOrDefault(parts[0].trim(), 10) : 10;
        int freqEnd = parts.length >= 2 ? parseIntOrDefault(parts[1].trim(), freqStart) : freqStart;
        int frameCount = parts.length >= 3 ? Math.max(0, parseIntOrDefault(parts[2].trim(), 0)) : 0;
        int mode = parts.length >= 4 ? Math.max(1, parseIntOrDefault(parts[3].trim(), 1)) : 1;
        return new DungeonSectionMeta(freqStart, freqEnd, frameCount, mode);
    }

    private record DungeonSectionMeta(int frequencyStart, int frequencyEnd, int frameCount, int mode) {
    }

    private static Pulse parseJson(String jsonText) {
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

    private static Pulse parseKeyValue(String text) {
        String name = "";
        Pulse.Channel channel = Pulse.Channel.A;
        int seconds = 5;
        List<String> frames = new ArrayList<>();

        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }

            String key = line.substring(0, idx).trim().toLowerCase();
            String value = line.substring(idx + 1).trim();

            switch (key) {
                case "name" -> name = value;
                case "channel" -> channel = Pulse.Channel.from(value);
                case "seconds" -> seconds = parseIntOrDefault(value, 5);
                case "frames" -> {
                    for (String token : value.split(",")) {
                        String frame = token.trim();
                        if (!frame.isEmpty()) {
                            frames.add(frame);
                        }
                    }
                }
                default -> {
                }
            }
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

    private static int parseIntOrDefault(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
