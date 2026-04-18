package org.nooberic.dglib.network;

import com.mojang.logging.LogUtils;
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.pulse.Pulse;
import org.nooberic.dglib.service.DeviceStatus;
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
            String payload = packet.getPayload();

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
                case PLAY_PULSE_BY_ID:
                    Pulse pulse = DgLibApi.get().getPulse(payload);
                    if (pulse == null) {
                        error = "pulse-id-not-registered:" + payload;
                        success = false;
                    } else {
                        Pulse runtimePulse = value > 0
                                ? new Pulse(
                                pulse.getName(),
                                pulse.getChannel(),
                                value,
                            pulse.getFrames()
                        )
                                : pulse;
                        success = DgLibApi.get().playPulse(runtimePulse);
                    }
                    break;
                case CONTROL:
                    int sep = payload == null ? -1 : payload.indexOf('|');
                    if (sep <= 0 || sep >= payload.length() - 1) {
                        error = "invalid-control-payload";
                        success = false;
                        break;
                    }
                    int strength;
                    try {
                        strength = Integer.parseInt(payload.substring(0, sep));
                    } catch (NumberFormatException ex) {
                        error = "invalid-control-strength";
                        success = false;
                        break;
                    }
                    String pulseId = payload.substring(sep + 1);
                    success = DgLibApi.get().control(channel, strength, pulseId, Math.max(1, value));
                    if (!success) {
                        error = "control-rejected-or-failed";
                    }
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
