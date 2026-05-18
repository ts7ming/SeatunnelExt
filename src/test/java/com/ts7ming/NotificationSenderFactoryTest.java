package com.ts7ming;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for NotificationSenderFactory
 */
public class NotificationSenderFactoryTest {

    @Test
    public void testCreateDingTalkSender() {
        NotificationConfig config = new NotificationConfig();
        config.setNotificationType("dingtalk");
        config.setDingtalkWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");
        config.setDingtalkSecret("test_secret");
        
        NotificationSender sender = NotificationSenderFactory.createSender(config);
        
        assertNotNull(sender);
        assertTrue(sender instanceof DingTalkNotificationSender);
    }

    @Test
    public void testCreateWechatSender() {
        NotificationConfig config = new NotificationConfig();
        config.setNotificationType("wechat");
        config.setWechatWebhookUrl("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test");
        
        NotificationSender sender = NotificationSenderFactory.createSender(config);
        
        assertNotNull(sender);
        assertTrue(sender instanceof WechatNotificationSender);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateDingTalkSenderMissingUrl() {
        NotificationConfig config = new NotificationConfig();
        config.setNotificationType("dingtalk");
        // Missing webhook URL and secret
        
        NotificationSenderFactory.createSender(config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWechatSenderMissingUrl() {
        NotificationConfig config = new NotificationConfig();
        config.setNotificationType("wechat");
        // Missing webhook URL
        
        NotificationSenderFactory.createSender(config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSenderWithInvalidType() {
        NotificationConfig config = new NotificationConfig();
        config.setNotificationType("invalid_type");
        
        NotificationSenderFactory.createSender(config);
    }
}
