package com.ts7ming;

import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * WeChat notification sender implementation
 */
@Slf4j
public class WechatNotificationSender implements NotificationSender {
    private final String webhookUrl;

    public WechatNotificationSender(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void sendMessage(String message) {
        try {
            String escapedMessage = message.replace("\\", "\\\\")
                                          .replace("\"", "\\\"")
                                          .replace("\n", "\\n")
                                          .replace("\r", "\\r")
                                          .replace("\t", "\\t");
            
            String jsonPayload = String.format("{\"msgtype\":\"text\",\"text\":{\"content\":\"%s\"}}", escapedMessage);
            
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                log.info("WeChat message sent successfully: {}", message);
            } else {
                log.error("WeChat message send failed, response code: {}, message: {}", responseCode, message);
            }
        } catch (Exception e) {
            log.error("Failed to send WeChat message: {}", message, e);
        }
    }
}
