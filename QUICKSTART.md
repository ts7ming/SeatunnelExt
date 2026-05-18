# Quick Start Guide

## Overview
This plugin allows SeaTunnel to send notifications via DingTalk or WeChat when job events occur (e.g., job failures).

## What Changed
The project has been refactored to use a **single configuration file** instead of command-line parameters. This provides:
- Cleaner configuration management
- Support for both DingTalk and WeChat
- Easier deployment and maintenance

## Migration from Old Version

### Old Approach (Deprecated)
```bash
sh bin/seatunnel.sh --async \
  -Ddingtalk.webhook.url="https://oapi.dingtalk.com/robot/send?access_token=XXX" \
  -Ddingtalk.secret="SECRET" \
  --config job.conf
```

### New Approach (Recommended)
1. Create a configuration file (e.g., `notification.properties`)
2. Use `-Dext.config.path` parameter

```bash
sh bin/seatunnel.sh --async \
  -Dext.config.path="/path/to/notification.properties" \
  --config job.conf
```

## Configuration File Examples

### For DingTalk
Create a file named `notification.properties`:
```properties
# Notification type: dingtalk or wechat
notification.type=dingtalk

# DingTalk Configuration
dingtalk.webhook.url=https://oapi.dingtalk.com/robot/send?access_token=YOUR_ACCESS_TOKEN
dingtalk.secret=YOUR_SECRET
```

### For WeChat
Create a file named `notification.properties`:
```properties
# Notification type: dingtalk or wechat
notification.type=wechat

# WeChat Configuration
wechat.webhook.url=https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY
```

## Deployment Steps

### 1. Build the Plugin
```bash
mvn clean package
```

### 2. Deploy the JAR
Copy the JAR file to SeaTunnel's lib directory:
```bash
cp target/SeatunnelExt-1.0-SNAPSHOT.jar /opt/apache-seatunnel/lib/
```

### 3. Create Configuration File
Create your notification configuration file:
```bash
mkdir -p /opt/apache-seatunnel/config
vi /opt/apache-seatunnel/config/notification.properties
```

Add your configuration (see examples above).

### 4. Restart SeaTunnel
```bash
systemctl stop seatunnel-master.service
systemctl stop seatunnel-worker.service
systemctl start seatunnel-master.service
systemctl start seatunnel-worker.service
```

### 5. Verify Plugin Loading
Check the logs:
```bash
grep "UnifiedNotificationEventListener" /opt/apache-seatunnel/logs/seatunnel-engine-master.log
```

You should see:
```
INFO Loaded event handlers: [com.ts7ming.UnifiedNotificationEventListener@..., ...]
```

### 6. Run Your Job
```bash
sh bin/seatunnel.sh --async \
  -Dext.config.path="/opt/apache-seatunnel/config/notification.properties" \
  --config your-job.conf \
  -n "Your Job Name"
```

## Configuration Parameters

| Parameter | Description | Required | Example |
|-----------|-------------|----------|---------|
| `notification.type` | Type of notification: `dingtalk` or `wechat` | Yes | `dingtalk` |
| `dingtalk.webhook.url` | DingTalk webhook URL | When type=dingtalk | `https://oapi.dingtalk.com/robot/send?access_token=XXX` |
| `dingtalk.secret` | DingTalk secret for signature | When type=dingtalk | `SECxxx` |
| `wechat.webhook.url` | WeChat webhook URL | When type=wechat | `https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=XXX` |

## Supported Events

Currently, the plugin handles:
- **JOB_STATUS**: Job failure notifications
  - Sends alert when job status is `FAILED`
  - Can be extended to handle other statuses (FINISHED, etc.)

Future support (commented in code):
- Schema change events (ADD_COLUMN, DROP_COLUMN, MODIFY_COLUMN, UPDATE_COLUMNS)
- Reader/Writer open/close events

## Troubleshooting

### Plugin Not Loading
1. Check if JAR is in `/opt/apache-seatunnel/lib/`
2. Verify SPI configuration file exists: `META-INF/services/org.apache.seatunnel.api.event.EventHandler`
3. Check logs: `grep "Loaded event handlers" /opt/apache-seatunnel/logs/seatunnel-engine-master.log`

### Configuration Not Loading
1. Verify the file path in `-Dext.config.path` is correct
2. Check file permissions: `ls -l /path/to/notification.properties`
3. Verify file format (should be standard Java properties format)
4. Check logs for error messages

### Messages Not Sending
1. Verify webhook URL is correct and accessible
2. Check network connectivity from SeaTunnel server to DingTalk/WeChat API
3. Verify secret/key is correct
4. Check logs for HTTP response codes and error messages

## Testing

Run unit tests:
```bash
mvn test
```

All tests should pass (9 tests total):
- 4 tests for NotificationConfig
- 5 tests for NotificationSenderFactory

## Project Structure

```
src/main/java/com/ts7ming/
├── UnifiedNotificationEventListener.java  # Main event listener
├── NotificationConfig.java                # Configuration loader
├── NotificationSender.java                # Sender interface
├── DingTalkNotificationSender.java        # DingTalk implementation
├── WechatNotificationSender.java          # WeChat implementation
└── NotificationSenderFactory.java         # Factory for creating senders

src/main/resources/META-INF/services/
└── org.apache.seatunnel.api.event.EventHandler  # SPI registration
```

## Benefits of New Approach

✅ **Single Configuration File**: All settings in one place  
✅ **Easy to Manage**: No long command lines  
✅ **Version Control Friendly**: Config files can be tracked in Git  
✅ **Multiple Environments**: Different configs for dev/staging/prod  
✅ **Flexible**: Easy to switch between DingTalk and WeChat  
✅ **Secure**: Config file permissions can be restricted  
✅ **Testable**: Comprehensive unit tests included  

## Need Help?

For issues or questions, please refer to:
- Main README.md
- Check logs in `/opt/apache-seatunnel/logs/`
- Review test cases in `src/test/java/`
