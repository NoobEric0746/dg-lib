package org.nooberic.dg_lib.util;

import java.util.UUID;

public final class QrCodeGenerator {
    private static final String DG_LAB_APP_URL = "https://www.dungeon-lab.com/app-download.php";
    private static final String DG_LAB_SOCKET_TAG = "DGLAB-SOCKET";

    private QrCodeGenerator() {
    }

    /**
     * 按照 DG-LAB 协议格式生成二维码内容（不包含 QR 编码，只是 URL 字符串）
     * 
     * @param wsUrl WebSocket 完整 URL（含 clientId），e.g. ws://127.0.0.1:9999/abc-123
     * @return 二维码应包含的内容 URL
     */
    public static String generateQrContent(String wsUrl) {
        return DG_LAB_APP_URL + "#" + DG_LAB_SOCKET_TAG + "#" + wsUrl;
    }

    /**
     * 为 WebSocket 连接生成唯一的 clientId（UUID 简化版）
     */
    public static String generateClientId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 生成完整的 WebSocket 连接 URL，带上 clientId
     * 
     * @param baseWsUrl 基础 URL，e.g. ws://127.0.0.1:9999
     * @param clientId 客户端 ID
     * @return 完整 URL，e.g. ws://127.0.0.1:9999/clientId
     */
    public static String generateWebSocketUrl(String baseWsUrl, String clientId) {
        String base = baseWsUrl.trim();
        if (base.endsWith("/")) {
            return base + clientId;
        }
        return base + "/" + clientId;
    }
}
