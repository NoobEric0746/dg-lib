package org.nooberic.dg_lib.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class DgProtocolCodec {
    private DgProtocolCodec() {
    }

    public static DgSocketMessage decode(String jsonText) {
        JsonObject obj = JsonParser.parseString(jsonText).getAsJsonObject();
        return new DgSocketMessage(
                readString(obj, "type"),
                readString(obj, "clientId"),
                readString(obj, "targetId"),
                readString(obj, "message")
        );
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
