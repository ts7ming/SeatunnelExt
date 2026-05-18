# Seatunnel - Notification Plugin

## Overview
#### Background
- Using SeaTunnel for data synchronization. Deployment steps reference
  
#### Problem
- Need to send DingTalk or WeChat notifications when tasks fail or other key events occur
- SeaTunnel itself doesn't support message notifications, must rely on DolphinScheduler or other external tools

#### Solution
- Utilize SeaTunnel's event listener functionality
- Write custom plugin to capture error events and send message notifications
- Group robot configuration through command line submission

#### Features
- **Unified Configuration**: Single configuration file for both DingTalk and WeChat
- **Flexible Notification Type**: Support both DingTalk and WeChat notifications
- **File-based Configuration**: All settings in one properties file
- **Easy Deployment**: Plugin-based architecture, no code changes needed

## Configuration

### Configuration File Format
Create a properties file (e.g., `notification-config.properties`) with the following structure:

```properties
# Notification type: dingtalk or wechat
notification.type=dingtalk

# DingTalk Configuration (required when notification.type=dingtalk)
dingtalk.webhook.url=https://oapi.dingtalk.com/robot/send?access_token=YOUR_ACCESS_TOKEN
dingtalk.secret=YOUR_SECRET

# WeChat Configuration (required when notification.type=wechat)
wechat.webhook.url=https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY
```

### Configuration Parameters
| Parameter | Description | Required |
|-----------|-------------|----------|
| `notification.type` | Notification channel type: `dingtalk` or `wechat` | Yes |
| `dingtalk.webhook.url` | DingTalk webhook URL | When type=dingtalk |
| `dingtalk.secret` | DingTalk secret for signature | When type=dingtalk |
| `wechat.webhook.url` | WeChat webhook URL | When type=wechat |

## Development

### Project Structure
- Package name `com.ts7ming` can be customized
```bash
│  pom.xml
│  config-example.properties
│
└─src
    └─main
        ├─java
        │  └─com
        │      └─ts7ming
        │              UnifiedNotificationEventListener.java
        │              NotificationConfig.java
        │              NotificationSender.java
        │              DingTalkNotificationSender.java
        │              WechatNotificationSender.java
        │              NotificationSenderFactory.java
        │
        └─resources
            └─META-INF
                └─services
                        org.apache.seatunnel.api.event.EventHandler
```

### Supported Events
SeaTunnel supports the following events:

| Event Type | Description | Associated Event Class |
|------------|-------------|------------------------|
| JOB_STATUS | Job status change event | JobStateEvent |
| SCHEMA_CHANGE_UPDATE_COLUMNS | Table structure update event | AlterTableColumnsEvent |
| SCHEMA_CHANGE_ADD_COLUMN | Table add column event | AlterTableAddColumnEvent |
| SCHEMA_CHANGE_DROP_COLUMN | Table drop column event | AlterTableDropColumnEvent |
| SCHEMA_CHANGE_MODIFY_COLUMN | Table modify column event | AlterTableModifyColumnEvent |
| READER_OPEN | Reader open event | ReaderOpenEvent |
| READER_CLOSE | Reader close event | ReaderCloseEvent |
| WRITER_OPEN | Writer open event | WriterOpenEvent |
| WRITER_CLOSE | Writer close event | WriterCloseEvent |

### Build
```bash
mvn clean package
```

## Deployment

### Download Pre-built JAR (if you don't want to build yourself)
```bash
cd /opt/apache-seatunnel/lib
wget https://github.com/ts7ming/SeatunnelExt/releases/download/v1/SeatunnelExt-1.0-SNAPSHOT.jar
# If network is slow, use gitee
wget https://gitee.com/ts7ming/SeatunnelExt/releases/download/v1/SeatunnelExt-1.0-SNAPSHOT.jar

# If downloaded with different user, check permissions
chown -R seatunnel:seatunnel /opt/apache-seatunnel
```

### Upload
- Upload the JAR file to SeaTunnel's lib directory (e.g., `/opt/apache-seatunnel/lib/`)

### Create Configuration File
Create your configuration file (e.g., `/opt/apache-seatunnel/config/notification.properties`):

For DingTalk:
```properties
notification.type=dingtalk
dingtalk.webhook.url=https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN
dingtalk.secret=YOUR_SECRET
```

For WeChat:
```properties
notification.type=wechat
wechat.webhook.url=https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY
```

**Note**: The configuration file is NOT read during plugin loading. It's only loaded when the first notification needs to be sent. This means:
- ✅ Plugin loads successfully even without `ext.config.path` parameter
- ✅ No warnings during SeaTunnel startup
- ✅ You can specify different config files for different jobs
- ✅ Configuration is loaded lazily on first use

### Restart SeaTunnel Service
```bash
systemctl stop seatunnel-master.service
systemctl stop seatunnel-worker.service

systemctl start seatunnel-master.service
systemctl start seatunnel-worker.service
```

### Verify Plugin Loading
```bash
grep "UnifiedNotificationEventListener" /opt/apache-seatunnel/logs/seatunnel-engine-master.log

# You should see output like:
INFO  [o.a.s.e.s.CoordinatorService  ] [pool-4-thread-1] - [localhost]:5801 [seatunnel] [5.1] Loaded event handlers: [com.ts7ming.UnifiedNotificationEventListener@20eaeaed, org.apache.seatunnel.api.event.LoggingEventHandler@59c99cb9]
```

**Important**: At this stage, you won't see any configuration loading messages because the config is loaded lazily. You'll see the configuration loading log only when the first job event triggers a notification.

## Running Tasks
> **Important**: The `-Dext.config.path` parameter must be placed before `--config` (due to SeaTunnel startup script parameter parsing order)

```bash
sh bin/seatunnel.sh --async -Dext.config.path="/opt/apache-seatunnel/config/notification.properties" --config job-config.conf -n "Job Name"
```

## Migration from Old Version
If you were using the old version with `-Ddingtalk.webhook.url` and `-Ddingtalk.secret` parameters:

1. Create a configuration file as shown above
2. Move your webhook URL and secret to the configuration file
3. Update your command to use `-Dext.config.path` instead of individual parameters

Old command:
```bash
sh bin/seatunnel.sh --async -Ddingtalk.webhook.url="..." -Ddingtalk.secret="..." --config job.conf
```

New command:
```bash
sh bin/seatunnel.sh --async -Dext.config.path="/path/to/config.properties" --config job.conf
```

## Done!
