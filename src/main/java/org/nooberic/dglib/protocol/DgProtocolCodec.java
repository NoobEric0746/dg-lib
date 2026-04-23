package org.nooberic.dglib.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.nooberic.dglib.pulse.Pulse;

import java.util.Locale;

public final class DgProtocolCodec {
    private DgProtocolCodec() {
    }

    public static DgSocketMessage decode(String jsonText) {
        JsonElement root = JsonParser.parseString(jsonText);
        if (!root.isJsonObject()) {
            return decodeNonObject(root);
        }
        JsonObject obj = root.getAsJsonObject();
        return new DgSocketMessage(
                readString(obj, "type"),
                readString(obj, "clientId"),
                readString(obj, "targetId"),
                readString(obj, "message"),
                true
        );
    }

    private static DgSocketMessage decodeNonObject(JsonElement root) {
        if (root == null || root.isJsonNull()) {
            return new DgSocketMessage("text", "", "", "", false);
        }
        if (root instanceof JsonPrimitive primitive && primitive.isString()) {
            return new DgSocketMessage("text", "", "", primitive.getAsString(), false);
        }
        return new DgSocketMessage("text", "", "", root.toString(), false);
    }

    public static String encodeStrengthDecrease(String clientId, String targetId, int channel, int delta) {
        return encodeStrengthMessage(1, clientId, targetId, channel, delta);
    }

    public static String encodeStrengthIncrease(String clientId, String targetId, int channel, int delta) {
        return encodeStrengthMessage(2, clientId, targetId, channel, delta);
    }

    public static String encodeStrengthSet(String clientId, String targetId, int channel, int value) {
        JsonObject payload = baseControlPayload(3, clientId, targetId);
        payload.addProperty("channel", channel);
        payload.addProperty("strength", clamp(value));
        return payload.toString();
    }

    public static String encodeClientWaveMessage(String clientId, String targetId, String channel, int seconds, String[] waveFrames) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "clientMsg");
        payload.addProperty("clientId", clientId);
        payload.addProperty("targetId", targetId);

        String normalizedChannel = "B".equalsIgnoreCase(channel) ? "B" : "A";
        payload.addProperty("channel", normalizedChannel);
        payload.addProperty("time", Math.max(1, Math.min(60, seconds)));
        payload.addProperty("message", normalizedChannel + ":" + toJsonArrayText(waveFrames));
        return payload.toString();
    }

    public static String encodePulseMessage(String clientId, String targetId, Pulse pulse) {
        if (pulse == null) {
            throw new IllegalArgumentException("Pulse cannot be null");
        }
        pulse.validate();
        String channel = pulse.getChannel() == Pulse.Channel.B ? "B" : "A";
        String[] frames = pulse.getFrames().stream()
                .map(frame -> frame.toUpperCase(Locale.ROOT))
                .toArray(String[]::new);
        return encodeClientWaveMessage(clientId, targetId, channel, pulse.getSeconds(), frames);
    }

    public static String encodeClearWaveMessage(String clientId, String targetId, String channel) {
        JsonObject payload = baseControlPayload(4, clientId, targetId);
        String normalizedChannel = "B".equalsIgnoreCase(channel) ? "B" : "A";
        payload.addProperty("message", "clear-" + ("B".equals(normalizedChannel) ? "2" : "1"));
        return payload.toString();
    }

    private static String encodeStrengthMessage(int type, String clientId, String targetId, int channel, int delta) {
        JsonObject payload = baseControlPayload(type, clientId, targetId);
        payload.addProperty("channel", channel);
        payload.addProperty("message", "set channel");
        payload.addProperty("strength", clamp(delta));
        return payload.toString();
    }

    private static JsonObject baseControlPayload(int type, String clientId, String targetId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", type);
        payload.addProperty("clientId", clientId);
        payload.addProperty("targetId", targetId);
        payload.addProperty("message", "set channel");
        return payload;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(200, value));
    }

    private static String toJsonArrayText(String[] waveFrames) {
        JsonArray arr = new JsonArray();
        for (String frame : waveFrames) {
            arr.add(frame);
        }
        return arr.toString();
    }

    private static String readString(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return element.toString();
    }
}
