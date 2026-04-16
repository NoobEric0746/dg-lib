package org.nooberic.dg_lib.client;

public interface WsTransportClient {
    void connect(String wsUrl, int timeoutMs);

    void sendText(String payload);

    void close();

    void setListener(Listener listener);

    interface Listener {
        void onOpen();

        void onText(String text);

        void onClose(int statusCode, String reason);

        void onError(Throwable throwable);
    }
}
