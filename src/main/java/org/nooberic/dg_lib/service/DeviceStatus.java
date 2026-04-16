package org.nooberic.dg_lib.service;

public class DeviceStatus {
    private final String clientId;
    private final String targetId;
    private final int channelAStrength;
    private final int channelBStrength;
    private final int channelALimit;
    private final int channelBLimit;
    private final String lastErrorCode;
    private final String wsUrl;

    public DeviceStatus(
            String clientId,
            String targetId,
            int channelAStrength,
            int channelBStrength,
            int channelALimit,
            int channelBLimit,
            String lastErrorCode,
            String wsUrl
    ) {
        this.clientId = clientId;
        this.targetId = targetId;
        this.channelAStrength = channelAStrength;
        this.channelBStrength = channelBStrength;
        this.channelALimit = channelALimit;
        this.channelBLimit = channelBLimit;
        this.lastErrorCode = lastErrorCode;
        this.wsUrl = wsUrl;
    }

    public static DeviceStatus empty() {
        return new DeviceStatus("", "", 0, 0, 0, 0, "", "");
    }

    public String getClientId() {
        return clientId;
    }

    public String getTargetId() {
        return targetId;
    }

    public int getChannelAStrength() {
        return channelAStrength;
    }

    public int getChannelBStrength() {
        return channelBStrength;
    }

    public int getChannelALimit() {
        return channelALimit;
    }

    public int getChannelBLimit() {
        return channelBLimit;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public String getWsUrl() {
        return wsUrl;
    }
}
