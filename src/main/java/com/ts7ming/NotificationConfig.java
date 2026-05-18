package com.ts7ming;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration class for notification settings
 */
@Data
@Slf4j
public class NotificationConfig {
    private String notificationType; // "dingtalk" or "wechat"
    private String dingtalkWebhookUrl;
    private String dingtalkSecret;
    private String wechatWebhookUrl;
    private String wechatSecret;

    /**
     * Load configuration from properties file
     * @param configPath Path to the configuration file
     * @return NotificationConfig instance
     */
    public static NotificationConfig loadFromPath(String configPath) {
        NotificationConfig config = new NotificationConfig();
        Properties props = new Properties();
        
        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
            
            config.setNotificationType(props.getProperty("notification.type", "dingtalk"));
            config.setDingtalkWebhookUrl(props.getProperty("dingtalk.webhook.url"));
            config.setDingtalkSecret(props.getProperty("dingtalk.secret"));
            config.setWechatWebhookUrl(props.getProperty("wechat.webhook.url"));
            config.setWechatSecret(props.getProperty("wechat.secret"));
            
            log.info("Configuration loaded from: {}", configPath);
            log.info("Notification type: {}", config.getNotificationType());
            
        } catch (IOException e) {
            log.error("Failed to load configuration from: {}", configPath, e);
            throw new RuntimeException("Failed to load configuration", e);
        }
        
        return config;
    }
}
