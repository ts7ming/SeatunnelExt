package com.ts7ming;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Unit tests for NotificationConfig
 */
public class NotificationConfigTest {

    @Test
    public void testLoadDingTalkConfig() throws IOException {
        // Create a temporary config file
        File tempFile = File.createTempFile("test-dingtalk", ".properties");
        tempFile.deleteOnExit();
        
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("notification.type=dingtalk\n");
            writer.write("dingtalk.webhook.url=https://oapi.dingtalk.com/robot/send?access_token=test_token\n");
            writer.write("dingtalk.secret=test_secret\n");
        }
        
        // Load configuration
        NotificationConfig config = NotificationConfig.loadFromPath(tempFile.getAbsolutePath());
        
        // Verify
        assertEquals("dingtalk", config.getNotificationType());
        assertEquals("https://oapi.dingtalk.com/robot/send?access_token=test_token", config.getDingtalkWebhookUrl());
        assertEquals("test_secret", config.getDingtalkSecret());
        assertNull(config.getWechatWebhookUrl());
        assertNull(config.getWechatSecret());
    }

    @Test
    public void testLoadWechatConfig() throws IOException {
        // Create a temporary config file
        File tempFile = File.createTempFile("test-wechat", ".properties");
        tempFile.deleteOnExit();
        
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("notification.type=wechat\n");
            writer.write("wechat.webhook.url=https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test_key\n");
        }
        
        // Load configuration
        NotificationConfig config = NotificationConfig.loadFromPath(tempFile.getAbsolutePath());
        
        // Verify
        assertEquals("wechat", config.getNotificationType());
        assertEquals("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test_key", config.getWechatWebhookUrl());
        assertNull(config.getDingtalkWebhookUrl());
        assertNull(config.getDingtalkSecret());
    }

    @Test(expected = RuntimeException.class)
    public void testLoadNonExistentFile() {
        NotificationConfig.loadFromPath("non-existent-file.properties");
    }

    @Test
    public void testDefaultNotificationType() throws IOException {
        // Create a temporary config file without notification.type
        File tempFile = File.createTempFile("test-default", ".properties");
        tempFile.deleteOnExit();
        
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("dingtalk.webhook.url=https://oapi.dingtalk.com/robot/send?access_token=test\n");
            writer.write("dingtalk.secret=secret\n");
        }
        
        // Load configuration - should default to dingtalk
        NotificationConfig config = NotificationConfig.loadFromPath(tempFile.getAbsolutePath());
        
        // Verify default type
        assertEquals("dingtalk", config.getNotificationType());
    }
}
