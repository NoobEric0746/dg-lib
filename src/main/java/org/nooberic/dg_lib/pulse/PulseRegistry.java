package org.nooberic.dg_lib.pulse;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PulseRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Pulse> REGISTRY = new ConcurrentHashMap<>();

    private PulseRegistry() {
    }

    public static boolean registerFromResource(String id, String fileName) {
        String key = normalizeId(id);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Pulse id cannot be empty");
        }

        String resourceName = normalizeFileName(fileName);
        String path = "pulse/" + resourceName;

        try (InputStream in = PulseRegistry.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                LOGGER.warn("Pulse resource not found: {}", path);
                return false;
            }
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Pulse pulse = PulseFileParser.parse(content);
            REGISTRY.put(key, pulse);
            LOGGER.info("Pulse registered: id={}, file={}", key, resourceName);
            return true;
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("Failed to register pulse from resource: id={}, file={}", key, resourceName, ex);
            return false;
        }
    }

    public static void register(String id, Pulse pulse) {
        String key = normalizeId(id);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Pulse id cannot be empty");
        }
        if (pulse == null) {
            throw new IllegalArgumentException("Pulse cannot be null");
        }
        pulse.validate();
        REGISTRY.put(key, pulse);
    }

    public static Pulse get(String id) {
        return REGISTRY.get(normalizeId(id));
    }

    public static boolean contains(String id) {
        return REGISTRY.containsKey(normalizeId(id));
    }

    public static Map<String, Pulse> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.trim().toLowerCase();
    }

    private static String normalizeFileName(String fileName) {
        String name = fileName == null ? "" : fileName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Pulse file name cannot be empty");
        }
        return name.endsWith(".frame") ? name : name + ".frame";
    }
}
