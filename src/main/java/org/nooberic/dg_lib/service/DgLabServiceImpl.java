package org.nooberic.dg_lib.service;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.nooberic.dg_lib.Config;
import org.nooberic.dg_lib.client.JdkWsTransportClient;
import org.nooberic.dg_lib.client.WsTransportClient;
import org.nooberic.dg_lib.protocol.DgProtocolCodec;
import org.nooberic.dg_lib.protocol.DgSocketMessage;
import org.nooberic.dg_lib.util.QrCodeGenerator;
import org.slf4j.Logger;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DgLabServiceImpl implements DgLabService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] BASIC_WAVE_V3 = new String[]{
            "0A0A0A0A00000000",
            "0A0A0A0A14141414",
            "0A0A0A0A28282828",
            "0A0A0A0A3C3C3C3C",
            "0A0A0A0A50505050",
            "0A0A0A0A64646464",
            "0A0A0A0A64646464",
            "0A0A0A0A64646464",
            "0A0A0A0A00000000",
            "0A0A0A0A00000000",
            "0A0A0A0A00000000"
    };

    private final WsTransportClient transportClient;
    private final ScheduledExecutorService reconnectScheduler;
    private final AtomicBoolean initialized;
    private final AtomicBoolean manualDisconnect;
    private final AtomicInteger reconnectAttempt;

    private volatile ConnectionState state;
    private volatile String clientId;
    private volatile String generatedWsUrl;
    private volatile String targetId;
    private volatile String wsUrl;
    private volatile int channelAStrength;
    private volatile int channelBStrength;
    private volatile int channelALimit;
    private volatile int channelBLimit;
    private volatile int channelAPainStrength;
    private volatile int channelBPainStrength;
    private volatile int channelASensationLowerLimit;
    private volatile int channelBSensationLowerLimit;
    private volatile String lastErrorCode;
    private volatile StrengthFeedbackListener strengthFeedbackListener;

    public DgLabServiceImpl() {
        this(new JdkWsTransportClient());
    }

    public DgLabServiceImpl(WsTransportClient transportClient) {
        this.transportClient = transportClient;
        this.reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        this.initialized = new AtomicBoolean(false);
        this.manualDisconnect = new AtomicBoolean(false);
        this.reconnectAttempt = new AtomicInteger(0);
        this.state = ConnectionState.DISCONNECTED;
        this.clientId = "";
        this.generatedWsUrl = "";
        this.targetId = "";
        this.wsUrl = "";
        this.lastErrorCode = "";
        this.channelAPainStrength = 0;
        this.channelBPainStrength = 0;
        this.channelASensationLowerLimit = 0;
        this.channelBSensationLowerLimit = 0;
        this.strengthFeedbackListener = null;
        this.transportClient.setListener(new TransportListener());
    }

    @Override
    public void initialize() {
        initialized.compareAndSet(false, true);
        channelAPainStrength = Config.getPainStrength(1);
        channelBPainStrength = Config.getPainStrength(2);
        channelASensationLowerLimit = Config.getSensationFloor(1);
        channelBSensationLowerLimit = Config.getSensationFloor(2);
    }

    @Override
    public void shutdown() {
        manualDisconnect.set(true);
        transportClient.close();
        state = ConnectionState.DISCONNECTED;
    }

    @Override
    public void connect() {
        if (!initialized.get()) {
            initialize();
        }
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING || state == ConnectionState.PAIRED) {
            LOGGER.info("DG-LAB already connecting or connected, state: {}", state);
            return;
        }

        manualDisconnect.set(false);
        state = ConnectionState.CONNECTING;

        String baseWsUrl = Config.wsUrl == null ? "" : Config.wsUrl.trim();
        clientId = "";
        targetId = "";

        if (isOfficialRelayEndpoint(baseWsUrl)) {
            // Public relay mode: connect root endpoint, wait for bind(clientId), then build QR URL.
            generatedWsUrl = normalizeBaseWsUrl(baseWsUrl);
            wsUrl = "";
            LOGGER.info("DG-LAB public relay mode, waiting for server-assigned clientId. connectUrl={}", generatedWsUrl);
        } else {
            // Local/mock mode: keep path-based URL generation with local clientId.
            if (generatedWsUrl.isEmpty()) {
                String newClientId = QrCodeGenerator.generateClientId();
                generatedWsUrl = QrCodeGenerator.generateWebSocketUrl(baseWsUrl, newClientId);
                LOGGER.info("DG-LAB generated new local session wsUrl: {}", generatedWsUrl);
            } else {
                LOGGER.info("DG-LAB reusing existing wsUrl: {}", generatedWsUrl);
            }
            wsUrl = generatedWsUrl;
        }

        LOGGER.info("DG-LAB initiating connection to: {}", generatedWsUrl);
        transportClient.connect(generatedWsUrl, Config.connectTimeoutMs);
    }

    @Override
    public void disconnect() {
        manualDisconnect.set(true);
        transportClient.close();
        state = ConnectionState.DISCONNECTED;
        generatedWsUrl = ""; // 清空 wsUrl，下次 connect 时会生成新的
        clientId = "";
        targetId = "";
    }

    @Override
    public ConnectionState getConnectionState() {
        return state;
    }

    @Override
    public boolean isPaired() {
        return state == ConnectionState.PAIRED && !clientId.isEmpty() && !targetId.isEmpty();
    }

    @Override
    public DeviceStatus getStatus() {
        return new DeviceStatus(
                clientId,
                targetId,
                channelAStrength,
                channelBStrength,
                channelALimit,
                channelBLimit,
            channelAPainStrength,
            channelBPainStrength,
            channelASensationLowerLimit,
            channelBSensationLowerLimit,
                lastErrorCode,
                wsUrl
        );
    }

    @Override
    public int getCurrentStrength(int channel) {
        return normalizeChannel(channel) == 2 ? channelBStrength : channelAStrength;
    }

    @Override
    public int getStrengthLimit(int channel) {
        return normalizeChannel(channel) == 2 ? channelBLimit : channelALimit;
    }

    @Override
    public int getPainStrength(int channel) {
        return normalizeChannel(channel) == 2 ? channelBPainStrength : channelAPainStrength;
    }

    @Override
    public void setPainStrength(int channel, int value) {
        int clamped = Math.max(0, Math.min(200, value));
        if (normalizeChannel(channel) == 2) {
            channelBPainStrength = clamped;
            Config.setPainStrength(2, clamped);
        } else {
            channelAPainStrength = clamped;
            Config.setPainStrength(1, clamped);
        }
    }

    @Override
    public int getSensationLowerLimit(int channel) {
        return normalizeChannel(channel) == 2 ? channelBSensationLowerLimit : channelASensationLowerLimit;
    }

    @Override
    public void setSensationLowerLimit(int channel, int value) {
        int clamped = Math.max(0, Math.min(200, value));
        if (normalizeChannel(channel) == 2) {
            channelBSensationLowerLimit = clamped;
            Config.setSensationFloor(2, clamped);
        } else {
            channelASensationLowerLimit = clamped;
            Config.setSensationFloor(1, clamped);
        }
    }

    @Override
    public void setStrengthFeedbackListener(StrengthFeedbackListener listener) {
        this.strengthFeedbackListener = listener;
    }

    @Override
    public boolean increaseStrength(int channel, int delta) {
        if (!isPaired() || delta <= 0) {
            return false;
        }
        transportClient.sendText(DgProtocolCodec.encodeStrengthIncrease(clientId, targetId, normalizeChannel(channel), delta));
        return true;
    }

    @Override
    public boolean decreaseStrength(int channel, int delta) {
        if (!isPaired() || delta <= 0) {
            return false;
        }
        transportClient.sendText(DgProtocolCodec.encodeStrengthDecrease(clientId, targetId, normalizeChannel(channel), delta));
        return true;
    }

    @Override
    public boolean setStrength(int channel, int value) {
        if (!isPaired()) {
            return false;
        }
        int normalizedChannel = normalizeChannel(channel);
        int safeValue = clampToSafetyLimit(normalizedChannel, value);
        transportClient.sendText(DgProtocolCodec.encodeStrengthSet(clientId, targetId, normalizedChannel, safeValue));
        if (safeValue != value) {
            LOGGER.info("DG-LAB setStrength adjusted by safety limit: channel={}, requested={}, applied={}", normalizedChannel, value, safeValue);
        }
        return true;
    }

    @Override
    public boolean playBasicWave(int channel, int seconds) {
        if (!isPaired()) {
            return false;
        }
        String channelName = normalizeChannel(channel) == 2 ? "B" : "A";
        String payload = DgProtocolCodec.encodeClientWaveMessage(clientId, targetId, channelName, seconds, BASIC_WAVE_V3);
        transportClient.sendText(payload);
        LOGGER.info("DG-LAB basic wave queued: channel={}, seconds={}", channelName, Math.max(1, Math.min(10, seconds)));
        return true;
    }

    private int normalizeChannel(int channel) {
        return channel == 2 ? 2 : 1;
    }

    private int clampToSafetyLimit(int channel, int requested) {
        int clampedRequested = Math.max(0, Math.min(200, requested));
        int safetyLimit = channel == 2 ? channelBLimit : channelALimit;
        if (safetyLimit > 0) {
            return Math.min(clampedRequested, safetyLimit);
        }
        return clampedRequested;
    }

    private void scheduleReconnect() {
        if (!Config.autoReconnect || manualDisconnect.get()) {
            return;
        }

        int attempt = reconnectAttempt.incrementAndGet();
        long exponential = (long) Config.reconnectBaseMs * (1L << Math.min(8, attempt - 1));
        long delayMs = Math.min(exponential, Config.reconnectMaxMs);

        reconnectScheduler.schedule(this::connect, delayMs, TimeUnit.MILLISECONDS);
        LOGGER.info("DG Lib reconnect scheduled in {} ms (attempt #{})", delayMs, attempt);
    }

    private void handleInboundMessage(String text) {
        try {
            DgSocketMessage message = DgProtocolCodec.decode(text);
            String type = message.getType();

            if ("bind".equals(type)) {
                String msg = message.getMessage();
                String cid = message.getClientId();
                String tid = message.getTargetId();
                
                if (!cid.isEmpty()) {
                    clientId = cid;
                    if (isOfficialRelayEndpoint(Config.wsUrl)) {
                        wsUrl = QrCodeGenerator.generateWebSocketUrl(Config.wsUrl, clientId);
                        LOGGER.info("DG-LAB public relay session URL ready: {}", wsUrl);
                    }
                    LOGGER.info("DG-LAB received clientId: {}", clientId);
                }
                
                if ("200".equals(msg)) {
                    targetId = tid;
                    state = ConnectionState.PAIRED;
                    LOGGER.info("DG-LAB paired successfully! targetId: {}", targetId);
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> org.nooberic.dg_lib.client.ClientPairingNotifier.onPairedSuccess());
                } else if (msg.isEmpty() || "targetId".equals(msg)) {
                    LOGGER.info("DG-LAB awaiting APP binding (clientId assigned)");
                } else {
                    LOGGER.warn("DG-LAB bind message with unknown code: {}", msg);
                }
                return;
            }

            if ("break".equals(type)) {
                targetId = "";
                state = ConnectionState.CONNECTED;
                LOGGER.info("DG-LAB connection broken, awaiting re-pair");
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> org.nooberic.dg_lib.client.ClientDisconnectNotifier.onDeviceDisconnected());
                return;
            }

            if ("error".equals(type)) {
                lastErrorCode = message.getMessage();
                LOGGER.warn("DG-LAB error code: {}", lastErrorCode);
                return;
            }

            if ("msg".equals(type)) {
                parseStrength(message.getMessage());
                LOGGER.debug("DG-LAB strength updated: A={}/{}, B={}/{}", 
                    channelAStrength, channelALimit, channelBStrength, channelBLimit);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to parse DG-LAB socket payload", ex);
        }
    }

    private void parseStrength(String messageBody) {
        if (messageBody == null || !messageBody.startsWith("strength-")) {
            return;
        }

        String raw = messageBody.substring("strength-".length());
        String[] parts = raw.split("\\+");
        if (parts.length < 4) {
            return;
        }

        channelAStrength = parseIntSafe(parts[0]);
        channelBStrength = parseIntSafe(parts[1]);
        channelALimit = parseIntSafe(parts[2]);
        channelBLimit = parseIntSafe(parts[3]);

        StrengthFeedbackListener listener = strengthFeedbackListener;
        if (listener != null) {
            try {
                listener.onStrengthFeedback(channelAStrength, channelBStrength, channelALimit, channelBLimit);
            } catch (Exception ex) {
                LOGGER.warn("DG-LAB strength feedback listener error", ex);
            }
        }
    }

    private int parseIntSafe(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private boolean isOfficialRelayEndpoint(String endpoint) {
        try {
            String raw = endpoint == null ? "" : endpoint.trim();
            if (raw.isEmpty()) {
                return false;
            }
            URI uri = URI.create(raw);
            String host = uri.getHost();
            return host != null && host.equalsIgnoreCase("ws.dungeon-lab.cn");
        } catch (Exception ignored) {
            return false;
        }
    }

    private String normalizeBaseWsUrl(String endpoint) {
        String base = endpoint == null ? "" : endpoint.trim();
        if (base.endsWith("/")) {
            return base;
        }
        return base + "/";
    }

    private class TransportListener implements WsTransportClient.Listener {
        @Override
        public void onOpen() {
            state = ConnectionState.CONNECTED;
            reconnectAttempt.set(0);
            channelAStrength = 0;
            channelBStrength = 0;
            channelALimit = 0;
            channelBLimit = 0;
            targetId = "";
            LOGGER.info("DG-LAB WebSocket CONNECTED, waiting for server bind message...");
            if (!wsUrl.isEmpty()) {
                LOGGER.info("QR Code URL ready: {}", wsUrl);
            } else {
                LOGGER.info("QR Code URL pending: waiting for bind(clientId)");
            }
        }

        @Override
        public void onText(String text) {
            handleInboundMessage(text);
        }

        @Override
        public void onClose(int statusCode, String reason) {
            LOGGER.info("DG-LAB websocket closed: code={}, reason={}", statusCode, reason);
            state = ConnectionState.DISCONNECTED;
            if (!manualDisconnect.get()) {
                scheduleReconnect();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            LOGGER.warn("DG-LAB websocket error", throwable);
            state = ConnectionState.DISCONNECTED;
            if (!manualDisconnect.get()) {
                scheduleReconnect();
            }
        }
    }
}
