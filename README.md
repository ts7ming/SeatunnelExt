# Seatunnel - 插件

## 说明
#### 背景
- 使用 Seatunnel 执行数据同步. 部署步骤参考
  
#### 问题
- 需要在任务报错或者其他关键事件发送钉钉/企业微信消息通知
- Seatunnel 本身不支持消息通知, 必须依赖 DolphinScheduler 或其他外部工具

#### 方案
- 利用SeaTunnel提供的事件监听器功能
- 编写自定义插件, 捕捉报错事件, 发送消息通知
- 群机器人配置通过命令行提交

#### 部署
- 如果不想写代码打包, 只需要报错通知, 跳过`开发插件` 步骤, 下载 jar包即可
- 如果需要自定义通知内容和其他事件处理, 自行调整代码

## 项目结构

本项目采用 Maven 多模块结构，包含两个独立的子项目：

```
SeatunnelExt/
├── pom.xml                          # 父 POM
├── seatunnel-dingtalk/              # 钉钉通知插件
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/ts7ming/DingTalkEventListener.java
│       │   └── resources/META-INF/services/org.apache.seatunnel.api.event.EventHandler
│       └── test/
└── seatunnel-wechat/                # 企业微信通知插件
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/ts7ming/WechatEventListener.java
        │   └── resources/META-INF/services/org.apache.seatunnel.api.event.EventHandler
        └── test/
```

### 子项目说明

#### 1. seatunnel-dingtalk (钉钉通知插件)
- **ArtifactId**: `seatunnel-dingtalk`
- **功能**: 监听 SeaTunnel 任务事件并发送钉钉群通知
- **生成 JAR**: `seatunnel-dingtalk-1.0-SNAPSHOT.jar`
- **配置参数**:
  - `dingtalk.webhook.url`: 钉钉机器人 Webhook URL
  - `dingtalk.secret`: 钉钉机器人密钥（可选）

#### 2. seatunnel-wechat (企业微信通知插件)
- **ArtifactId**: `seatunnel-wechat`
- **功能**: 监听 SeaTunnel 任务事件并发送企业微信群通知
- **生成 JAR**: `seatunnel-wechat-1.0-SNAPSHOT.jar`
- **配置参数**:
  - `wechat.webhook.url`: 企业微信机器人 Webhook URL
  - `wechat.secret`: 企业微信机器人密钥（可选，仅在启用加密时需要）

## 开发插件

##### 支持的事件类型

| 事件类型                 | 说明           | 关联事件类             |
| ---------------------------- | ---------------- | --------------------------- |
| JOB_STATUS                   | 作业状态变更事件 | JobStateEvent               |
| SCHEMA_CHANGE_UPDATE_COLUMNS | 表结构更新事件 | AlterTableColumnsEvent      |
| SCHEMA_CHANGE_ADD_COLUMN     | 表添加列事件 | AlterTableAddColumnEvent    |
| SCHEMA_CHANGE_DROP_COLUMN    | 表删除列事件 | AlterTableDropColumnEvent   |
| SCHEMA_CHANGE_MODIFY_COLUMN  | 表修改列事件 | AlterTableModifyColumnEvent |
| READER_OPEN                  | 读取器打开事件 | ReaderOpenEvent             |
| READER_CLOSE                 | 读取器关闭事件 | ReaderCloseEvent            |
| WRITER_OPEN                  | 写入器打开事件 | WriterOpenEvent             |
| WRITER_CLOSE                 | 写入器关闭事件 | WriterCloseEvent            |

##### 打包
```bash
# 构建所有子项目
mvn clean package

# 只构建钉钉插件
mvn clean package -pl seatunnel-dingtalk

# 只构建企业微信插件
mvn clean package -pl seatunnel-wechat
```

构建完成后，会在以下位置生成 JAR 包：
- `seatunnel-dingtalk/target/seatunnel-dingtalk-1.0-SNAPSHOT.jar`
- `seatunnel-wechat/target/seatunnel-wechat-1.0-SNAPSHOT.jar`

## 部署插件

##### 直接下载可用 jar (如果不想自己打包)
```bash
cd /opt/apache-seatunnel/lib
wget https://github.com/ts7ming/SeatunnelExt/releases/download/v1/seatunnel-dingtalk-1.0-SNAPSHOT.jar
wget https://github.com/ts7ming/SeatunnelExt/releases/download/v1/seatunnel-wechat-1.0-SNAPSHOT.jar
```

##### 上传
- 将需要的 JAR 包上传到 Seatunnel 根目录 lib 下 (例如: /opt/apache-seatunnel/lib/)
- **注意**: 可以同时部署两个插件，也可以只部署其中一个

##### 重启 Seatunnel 服务
```bash
systemctl stop seatunnel-master.service
systemctl stop seatunnel-worker.service

systemctl start seatunnel-master.service
systemctl start seatunnel-worker.service
```

##### 检查插件是否加载
```bash
# 检查钉钉插件
grep "DingTalk" /opt/apache-seatunnel/logs/seatunnel-engine-master.log

# 检查企业微信插件
grep "Wechat" /opt/apache-seatunnel/logs/seatunnel-engine-master.log

# 结果应该有如下信息
INFO  [o.a.s.e.s.CoordinatorService  ] [pool-4-thread-1] - [localhost]:5801 [seatunnel] [5.1] Loaded event handlers: [com.ts7ming.DingTalkEventListener@20eaeaed, com.ts7ming.WechatEventListener@59c99cb9, org.apache.seatunnel.api.event.LoggingEventHandler@xxxxx]
```

## 运行任务

> 注意: -D参数一定要在 --config前面 (SeaTunnel启动脚本解析参数的顺序不够灵活)

### 使用钉钉通知
```bash
sh bin/seatunnel.sh --async \
  -Ddingtalk.webhook.url="https://oapi.dingtalk.com/robot/send?access_token=钉钉群Token" \
  -Ddingtalk.secret="钉钉群secret" \
  --config 任务配置.conf \
  -n "任务名称"
```

### 使用企业微信通知
```bash
sh bin/seatunnel.sh --async \
  -Dwechat.webhook.url="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=企业微信机器人Key" \
  -Dwechat.secret="企业微信机器人密钥（可选）" \
  --config 任务配置.conf \
  -n "任务名称"
```

### 同时使用钉钉和企业微信
如果同时部署了两个插件，可以一次性配置两个通知渠道：
```bash
sh bin/seatunnel.sh --async \
  -Ddingtalk.webhook.url="https://oapi.dingtalk.com/robot/send?access_token=钉钉群Token" \
  -Ddingtalk.secret="钉钉群secret" \
  -Dwechat.webhook.url="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=企业微信机器人Key" \
  -Dwechat.secret="企业微信机器人密钥（可选）" \
  --config 任务配置.conf \
  -n "任务名称"
```

## 配置说明

### 环境变量方式（推荐）
除了通过 `-D` 系统属性配置外，还支持环境变量：

**钉钉插件**:
- `DINGTALK_WEBHOOK_URL`: 钉钉 Webhook URL
- `DINGTALK_SECRET`: 钉钉密钥

**企业微信插件**:
- `WECHAT_WEBHOOK_URL`: 企业微信 Webhook URL
- `WECHAT_SECRET`: 企业微信密钥

配置优先级：**环境变量 > 系统属性 (-D) > 默认值**

### 安全建议
1. **不要硬编码敏感信息**：Webhook URL 和 Secret 应该通过环境变量或命令行参数传递
2. **生产环境推荐使用环境变量**：更安全且便于管理
3. **定期更换密钥**：提高安全性

## Done!