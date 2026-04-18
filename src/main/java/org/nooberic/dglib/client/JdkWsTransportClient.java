package org.nooberic.dglib.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

public class JdkWsTransportClient implements WsTransportClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private volatile Listener listener;
    private volatile WebSocket webSocket;

    @Override
    public void connect(String wsUrl, int timeoutMs) {
        LOGGER.info("DG-LAB WebSocket connecting to: {}", wsUrl);
        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .buildAsync(URI.create(wsUrl), new JdkListener())
                .whenComplete((ws, throwable) -> {
                    if (throwable != null) {
                        LOGGER.warn("DG-LAB WebSocket connection failed", throwable);
                        if (listener != null) {
                            listener.onError(throwable);
                        }
                    } else if (ws != null) {
                        webSocket = ws;
                        LOGGER.info("DG-LAB WebSocket connection successful");
                    }
                });
    }

    @Override
    public void sendText(String payload) {
        WebSocket ws = webSocket;
        if (ws != null) {
            ws.sendText(payload, true);
            LOGGER.debug("DG-LAB OUT: {}", payload);
        } else {
            LOGGER.warn("DG-LAB WebSocket not connected, cannot send: {}", payload);
        }
    }

    @Override
    public void close() {
        WebSocket ws = webSocket;
        webSocket = null;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "client-close");
        }
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private class JdkListener implements WebSocket.Listener {
        private final StringBuilder textBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            LOGGER.info("DG-LAB WebSocket onOpen triggered");
            webSocket.request(1);
            if (listener != null) {
                listener.onOpen();
            } else {
                LOGGER.warn("DG-LAB WebSocket listener not set in onOpen");
            }
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                String payload = textBuffer.toString();
                LOGGER.debug("DG-LAB IN: {}", payload);
                if (listener != null) {
                    listener.onText(payload);
                }
                textBuffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (listener != null) {
                listener.onClose(statusCode, Objects.requireNonNullElse(reason, ""));
            }
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            LOGGER.warn("DG-LAB WebSocket error", error);
            if (listener != null) {
                listener.onError(error);
            }
        }
    }
}
