package org.nooberic.dg_lib.protocol;

public class DgSocketMessage {
    private final String type;
    private final String clientId;
    private final String targetId;
    private final String message;

    public DgSocketMessage(String type, String clientId, String targetId, String message) {
        this.type = type;
        this.clientId = clientId;
        this.targetId = targetId;
        this.message = message;
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
}
