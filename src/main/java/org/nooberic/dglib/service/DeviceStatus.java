package org.nooberic.dglib.service;

public class DeviceStatus {
    private final String clientId;
    private final String targetId;
    private final int channelAStrength;
    private final int channelBStrength;
    private final int channelALimit;
    private final int channelBLimit;
    private final int channelAPainStrength;
    private final int channelBPainStrength;
    private final int channelASensationLowerLimit;
    private final int channelBSensationLowerLimit;
    private final String lastErrorCode;
    private final String wsUrl;

    public DeviceStatus(
            String clientId,
            String targetId,
            int channelAStrength,
            int channelBStrength,
            int channelALimit,
            int channelBLimit,
                int channelAPainStrength,
                int channelBPainStrength,
                int channelASensationLowerLimit,
                int channelBSensationLowerLimit,
            String lastErrorCode,
            String wsUrl
    ) {
        this.clientId = clientId;
        this.targetId = targetId;
        this.channelAStrength = channelAStrength;
        this.channelBStrength = channelBStrength;
        this.channelALimit = channelALimit;
        this.channelBLimit = channelBLimit;
        this.channelAPainStrength = channelAPainStrength;
        this.channelBPainStrength = channelBPainStrength;
        this.channelASensationLowerLimit = channelASensationLowerLimit;
        this.channelBSensationLowerLimit = channelBSensationLowerLimit;
        this.lastErrorCode = lastErrorCode;
        this.wsUrl = wsUrl;
    }

    public static DeviceStatus empty() {
        return new DeviceStatus("", "", 0, 0, 0, 0, 0, 0, 0, 0, "", "");
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

    public int getChannelAPainStrength() {
        return channelAPainStrength;
    }

    public int getChannelBPainStrength() {
        return channelBPainStrength;
    }

    public int getChannelASensationLowerLimit() {
        return channelASensationLowerLimit;
    }

    public int getChannelBSensationLowerLimit() {
        return channelBSensationLowerLimit;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public String getWsUrl() {
        return wsUrl;
    }
}
