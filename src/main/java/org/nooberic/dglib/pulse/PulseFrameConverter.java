package org.nooberic.dglib.pulse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Converts DG-LAB's Dungeonlab+pulse share format to 16-hex-character frame data. */
public final class PulseFrameConverter {
    private static final String PREFIX = "Dungeonlab+pulse:";
    private static final int[] FREQUENCIES = {
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
            50, 52, 54, 56, 58, 60, 62, 64, 66, 68, 70, 72, 74, 76, 78, 80, 85, 90, 95, 100,
            110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 233, 266, 300, 333, 366, 400,
            450, 500, 550, 600, 700, 800, 900, 1000
    };

    private PulseFrameConverter() {
    }

    public static List<String> convert(Path pulseFile) throws IOException {
        if (pulseFile == null) {
            throw new IllegalArgumentException("Pulse file cannot be null");
        }
        return convert(Files.readString(pulseFile, StandardCharsets.UTF_8));
    }

    public static List<String> convert(String pulseText) {
        if (pulseText == null || !pulseText.trim().regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            throw new IllegalArgumentException("Pulse data must begin with 'Dungeonlab+pulse:'");
        }

        String body = pulseText.trim().substring(PREFIX.length());
        int settingsSeparator = body.indexOf('=');
        if (settingsSeparator < 0) {
            throw new IllegalArgumentException("Pulse data is missing its settings separator");
        }

        List<String> frames = new ArrayList<>();
        String[] sections = body.substring(settingsSeparator + 1).split("\\+section\\+", -1);
        for (String section : sections) {
            appendSectionFrames(section, frames);
        }
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("Pulse data contains no enabled sections with shape points");
        }
        return List.copyOf(frames);
    }

    public static String toFrameJson(String pulseText) {
        List<String> frames = convert(pulseText);
        StringBuilder json = new StringBuilder("{\n  \"frames\": [\n");
        for (int index = 0; index < frames.size(); index++) {
            json.append("    \"").append(frames.get(index)).append('"');
            if (index + 1 < frames.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        return json.append("  ]\n}\n").toString();
    }

    public static void convertToFrameFile(Path pulseFile, Path frameFile) throws IOException {
        if (frameFile == null) {
            throw new IllegalArgumentException("Frame file cannot be null");
        }
        Files.writeString(frameFile, toFrameJson(Files.readString(pulseFile, StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    private static void appendSectionFrames(String section, List<String> frames) {
        int shapeSeparator = section.indexOf('/');
        if (shapeSeparator < 0) {
            throw new IllegalArgumentException("Pulse section is missing its shape separator");
        }
        String[] header = section.substring(0, shapeSeparator).split(",", -1);
        if (header.length < 5) {
            throw new IllegalArgumentException("Pulse section header requires five values");
        }
        if (parseInt(header[4], 1) == 0) {
            return;
        }

        int startFrequency = frequencyForIndex(parseInt(header[0], 0));
        int endFrequency = frequencyForIndex(parseInt(header[1], 0));
        int sectionDuration = clamp(parseInt(header[2], 0), 0, 99) + 1;
        int frequencyMode = clamp(parseInt(header[3], 1), 1, 4);
        List<Integer> strengths = parseStrengths(section.substring(shapeSeparator + 1));
        if (strengths.size() < 2) {
            throw new IllegalArgumentException("Pulse section requires at least two shape points");
        }

        int elementCount = Math.max(1, (int) Math.ceil((double) sectionDuration / strengths.size()));
        int actualDuration = elementCount * strengths.size();
        for (int elementIndex = 0; elementIndex < elementCount; elementIndex++) {
            for (int shapeIndex = 0; shapeIndex < strengths.size(); shapeIndex++) {
                int currentTime = elementIndex * strengths.size() + shapeIndex;
                int frequency = frequencyForMode(frequencyMode, startFrequency, endFrequency, elementIndex,
                        elementCount, shapeIndex, strengths.size(), currentTime, actualDuration);
                frames.add(encodeFrame(frequency, strengths.get(shapeIndex)));
            }
        }
    }

    private static List<Integer> parseStrengths(String shapeText) {
        List<Integer> strengths = new ArrayList<>();
        for (String point : shapeText.split(",", -1)) {
            String value = point.trim();
            if (value.isEmpty()) {
                continue;
            }
            int anchorSeparator = value.indexOf('-');
            String strengthText = anchorSeparator < 0 ? value : value.substring(0, anchorSeparator);
            try {
                strengths.add(clamp((int) Math.round(Double.parseDouble(strengthText.trim())), 0, 100));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid pulse strength: " + strengthText, exception);
            }
        }
        return strengths;
    }

    private static int frequencyForMode(int mode, int start, int end, int elementIndex, int elementCount,
                                        int shapeIndex, int shapeCount, int currentTime, int actualDuration) {
        return switch (mode) {
            case 2 -> outputValue(start + (end - start) * ((double) currentTime / actualDuration));
            case 3 -> outputValue(start + (end - start) * ((double) shapeIndex / shapeCount));
            case 4 -> outputValue(start + (end - start) * (elementCount > 1
                    ? (double) elementIndex / (elementCount - 1) : 0));
            default -> outputValue(start);
        };
    }

    private static int frequencyForIndex(int index) {
        return FREQUENCIES[clamp(index, 0, FREQUENCIES.length - 1)];
    }

    private static int outputValue(double frequency) {
        double value = frequency <= 100 ? frequency : frequency <= 600
                ? (frequency - 100) / 5 + 100 : (frequency - 600) / 10 + 200;
        return clamp((int) Math.round(value), 10, 240);
    }

    private static String encodeFrame(int frequency, int strength) {
        return String.format(Locale.ROOT, "%02X%02X%02X%02X%02X%02X%02X%02X", frequency, frequency, frequency,
                frequency, strength, strength, strength, strength);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int clamp(int value, int lowerBound, int upperBound) {
        return Math.max(lowerBound, Math.min(upperBound, value));
    }
}