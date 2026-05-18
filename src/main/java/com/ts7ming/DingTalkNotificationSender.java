package com.ts7ming;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * DingTalk notification sender implementation
 */
@Slf4j
public class DingTalkNotificationSender implements NotificationSender {
    private final String webhookUrl;
    private final String secret;

    public DingTalkNotificationSender(String webhookUrl, String secret) {
        this.webhookUrl = webhookUrl;
        this.secret = secret;
    }

    @Override
    public void sendMessage(String message) {
        try {
            long timestamp = System.currentTimeMillis();
            String sign = generateSign(timestamp, secret);
            String fullUrl = webhookUrl + "&timestamp=" + timestamp + "&sign=" + sign;
            
            String escapedMessage = message.replace("\\", "\\\\")
                                          .replace("\"", "\\\"")
                                          .replace("\n", "\\n")
                                          .replace("\r", "\\r")
                                          .replace("\t", "\\t");
            
            String jsonPayload = String.format("{\"msgtype\":\"text\",\"text\":{\"content\":\"%s\"}}", escapedMessage);
            
            URL url = new URL(fullUrl);
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
                log.info("DingTalk message sent successfully: {}", message);
            } else {
                log.error("DingTalk message send failed, response code: {}, message: {}", responseCode, message);
            }
        } catch (Exception e) {
            log.error("Failed to send DingTalk message: {}", message, e);
        }
    }

    private String generateSign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return URLEncoder.encode(new String(Base64.getEncoder().encode(signData)), "UTF-8");
    }
}
