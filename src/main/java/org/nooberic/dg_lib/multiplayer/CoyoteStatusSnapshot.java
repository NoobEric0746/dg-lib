package org.nooberic.dg_lib.multiplayer;

public class CoyoteStatusSnapshot {
    private final boolean paired;
    private final int channelAStrength;
    private final int channelBStrength;
    private final int channelALimit;
    private final int channelBLimit;
    private final int channelAPainStrength;
    private final int channelBPainStrength;
    private final int channelASensationLowerLimit;
    private final int channelBSensationLowerLimit;
    private final String error;
    private final long updatedAtMs;

    public CoyoteStatusSnapshot(
            boolean paired,
            int channelAStrength,
            int channelBStrength,
            int channelALimit,
            int channelBLimit,
                int channelAPainStrength,
                int channelBPainStrength,
                int channelASensationLowerLimit,
                int channelBSensationLowerLimit,
            String error,
            long updatedAtMs
    ) {
        this.paired = paired;
        this.channelAStrength = channelAStrength;
        this.channelBStrength = channelBStrength;
        this.channelALimit = channelALimit;
        this.channelBLimit = channelBLimit;
        this.channelAPainStrength = channelAPainStrength;
        this.channelBPainStrength = channelBPainStrength;
        this.channelASensationLowerLimit = channelASensationLowerLimit;
        this.channelBSensationLowerLimit = channelBSensationLowerLimit;
        this.error = error == null ? "" : error;
        this.updatedAtMs = updatedAtMs;
    }

    public static CoyoteStatusSnapshot empty() {
        return new CoyoteStatusSnapshot(false, 0, 0, 0, 0, 0, 0, 0, 0, "", 0L);
    }

    public boolean isPaired() {
        return paired;
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

    public String getError() {
        return error;
    }

    public long getUpdatedAtMs() {
        return updatedAtMs;
    }

    public int currentStrength(int channel) {
        return channel == 2 ? channelBStrength : channelAStrength;
    }

    public int strengthLimit(int channel) {
        return channel == 2 ? channelBLimit : channelALimit;
    }
}
