# Seatunnel - 插件

## 说明
#### 背景
- 使用 Seatunnel 执行数据同步. 部署步骤参考
  
#### 问题
- 需要在任务报错或者其他关键事件发送钉钉消息通知
- Seatunnel 本身不支持消息通知, 必须依赖 DolphinScheduler 或其他外部工具

#### 方案
- 利用SeaTunnel提供的事件监听器功能
- 编写自定义插件, 捕捉报错事件, 发送消息通知
- 群机器人配置通过命令行提交

#### 部署
- 如果不想写代码打包, 只需要报错通知, 跳过`开发插件` 步骤, 下载 jar包即可
- 如果需要自定义通知内容和其他事件处理, 自行调整代码

## 开发插件
##### 项目结构
- 包名 `com.ts7ming` 自定义即可
```bash
│  pom.xml
│
└─src
    └─main
        ├─java
        │  └─com
        │      └─ts7ming
        │              DingTalkEventListener.java
        │
        └─resources
            └─META-INF
                └─services
                        org.apache.seatunnel.api.event.EventHandler
```

##### DingTalkEventListener.java
- Seatunnel支持的事件如下
  
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
mvn clean package
```

## 部署插件
##### 直接下载可用 jar (如果不想自己打包)
```bash
cd /opt/apache-seatunnel/lib
wget https://github.com/ts7ming/SeatunnelExt/releases/download/v1/SeatunnelExt-1.0-SNAPSHOT.jar
# 网络差的话用 gitee
wget https://gitee.com/ts7ming/SeatunnelExt/releases/download/v1/SeatunnelExt-1.0-SNAPSHOT.jar

# 如果用其他用户下载, 注意权限
chown -R seatunnel:seatunnel /opt/apache-seatunnel
```

##### 上传
- Jar包上传到Seatunnel 根目录 lib 下 (例如: /opt/apache-seatunnel/lib/)

##### 重启 Seatunnel 服务
```bash
systemctl stop seatunnel-master.service
systemctl stop seatunnel-worker.service

systemctl start seatunnel-master.service
systemctl start seatunnel-worker.service
```
##### 检查插件是否加载
```bash
grep "DingTalk" /opt/apache-seatunnel/logs/seatunnel-engine-master.log

# 结果应该有如下信息
INFO  [o.a.s.e.s.CoordinatorService  ] [pool-4-thread-1] - [localhost]:5801 [seatunnel] [5.1] Loaded event handlers: [com.ts7ming.DingTalkEventListener@20eaeaed, org.apache.seatunnel.api.event.LoggingEventHandler@59c99cb9]
```

## 运行任务
> 注意: -D参数一定要在 --config前面 (SeaTunnel启动脚本解析参数的顺序不够灵活)

```bash
sh bin/seatunnel.sh --async -Ddingtalk.webhook.url="https://oapi.dingtalk.com/robot/send?access_token=钉钉群Token" -Ddingtalk.secret="钉钉群secret" --config 任务配置.conf  -n "任务名称"
```

## Done!
