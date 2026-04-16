package org.nooberic.dg_lib.network;

import com.mojang.logging.LogUtils;
import org.nooberic.dg_lib.api.DgLibApi;
import org.nooberic.dg_lib.service.DeviceStatus;
import org.slf4j.Logger;

public final class DgClientPacketExecutor {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DgClientPacketExecutor() {
    }

    public static void handleRequest(DgS2CClientOpRequestPacket packet) {
        boolean success = false;
        String error = "";

        try {
            DgClientOperation op = packet.getOperation();
            int channel = packet.getChannel();
            int value = packet.getValue();

            switch (op) {
                case QUERY_STATUS:
                    success = true;
                    break;
                case SET_STRENGTH:
                    success = DgLibApi.get().setStrength(channel, value);
                    break;
                case INCREASE_STRENGTH:
                    success = DgLibApi.get().increaseStrength(channel, Math.max(1, value));
                    break;
                case DECREASE_STRENGTH:
                    success = DgLibApi.get().decreaseStrength(channel, Math.max(1, value));
                    break;
                case PLAY_BASIC_WAVE:
                    success = DgLibApi.get().playBasicWave(channel, value);
                    break;
                default:
                    error = "unsupported-operation";
                    break;
            }

            if (!success && error.isEmpty()) {
                error = "client-operation-failed";
            }
        } catch (Throwable ex) {
            LOGGER.warn("DG-LAB client op execution error", ex);
            error = "client-exception:" + ex.getClass().getSimpleName();
        }

        DeviceStatus status = DgLibApi.get().getStatus();
        DgC2SClientOpResponsePacket response = new DgC2SClientOpResponsePacket(
                packet.getRequestId(),
                success,
                DgLibApi.get().isPaired(),
                status.getChannelAStrength(),
                status.getChannelBStrength(),
                status.getChannelALimit(),
                status.getChannelBLimit(),
                status.getChannelAPainStrength(),
                status.getChannelBPainStrength(),
                status.getChannelASensationLowerLimit(),
                status.getChannelBSensationLowerLimit(),
                error
        );
        DgNetworking.sendToServer(response);
    }
}
