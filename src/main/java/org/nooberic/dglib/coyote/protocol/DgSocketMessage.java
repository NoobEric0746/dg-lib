package org.nooberic.dglib.coyote.protocol;

public class DgSocketMessage {
    private final String type;
    private final String clientId;
    private final String targetId;
    private final String message;
    private final boolean structured;

    public DgSocketMessage(String type, String clientId, String targetId, String message) {
        this(type, clientId, targetId, message, true);
    }

    public DgSocketMessage(String type, String clientId, String targetId, String message, boolean structured) {
        this.type = type;
        this.clientId = clientId;
        this.targetId = targetId;
        this.message = message;
        this.structured = structured;
    }

    public String getType() {
        return type;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isStructured() {
        return structured;
    }
}
