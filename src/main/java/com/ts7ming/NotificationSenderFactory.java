package com.ts7ming;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating notification senders based on configuration
 */
@Slf4j
public class NotificationSenderFactory {
    
    /**
     * Create a notification sender based on configuration
     * @param config The notification configuration
     * @return NotificationSender instance
     */
    public static NotificationSender createSender(NotificationConfig config) {
        String type = config.getNotificationType().toLowerCase();
        
        switch (type) {
            case "dingtalk":
                if (config.getDingtalkWebhookUrl() == null || config.getDingtalkSecret() == null) {
                    throw new IllegalArgumentException("DingTalk webhook URL and secret must be configured");
                }
                log.info("Creating DingTalk notification sender");
                return new DingTalkNotificationSender(config.getDingtalkWebhookUrl(), config.getDingtalkSecret());
                
            case "wechat":
                if (config.getWechatWebhookUrl() == null) {
                    throw new IllegalArgumentException("WeChat webhook URL must be configured");
                }
                log.info("Creating WeChat notification sender");
                return new WechatNotificationSender(config.getWechatWebhookUrl());
                
            default:
                throw new IllegalArgumentException("Unsupported notification type: " + type);
        }
    }
}
