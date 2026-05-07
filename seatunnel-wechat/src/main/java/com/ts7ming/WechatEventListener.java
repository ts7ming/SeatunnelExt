package com.ts7ming;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.event.Event;
import org.apache.seatunnel.api.event.EventHandler;
import org.apache.seatunnel.api.event.EventType;
import org.apache.seatunnel.engine.common.job.JobStatus;
import org.apache.seatunnel.engine.common.job.JobStateEvent;
import org.apache.seatunnel.api.source.event.ReaderOpenEvent;
import org.apache.seatunnel.api.sink.event.WriterCloseEvent;
import org.apache.seatunnel.api.table.schema.event.AlterTableAddColumnEvent;
import org.apache.seatunnel.api.table.schema.event.AlterTableColumnsEvent;
import org.apache.seatunnel.api.table.schema.event.AlterTableDropColumnEvent;
import org.apache.seatunnel.api.table.schema.event.AlterTableModifyColumnEvent;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class WechatEventListener implements EventHandler {
    private static final String WEBHOOK_URL = System.getProperty("wechat.webhook.url", "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_ACCESS_TOKEN");
    private static final String SECRET = System.getProperty("wechat.secret", "");
    
    @Override
    public void handle(Event event) {
        EventType eventType = event.getEventType();
        if (eventType == EventType.JOB_STATUS) {
            handleJobStateEvent((JobStateEvent) event);
        } 
//        else if (eventType.name().equals("SCHEMA_CHANGE_ADD_COLUMN")) {
//            handleAddColumnEvent((AlterTableAddColumnEvent) event);
//        }
//        else if (eventType.name().equals("SCHEMA_CHANGE_UPDATE_COLUMNS")) {
//            handleUpdateColumnEvent((AlterTableColumnsEvent) event);
//        }
//        else if (eventType.name().equals("SCHEMA_CHANGE_DROP_COLUMN")) {
//            handleDropColumnEvent((AlterTableDropColumnEvent) event);
//        }
//        else if (eventType.name().equals("SCHEMA_CHANGE_MODIFY_COLUMN")) {
//            handleModifyColumnEvent((AlterTableModifyColumnEvent) event);
//        }
        else {
            log.debug("忽略未处理的事件类型: {}", eventType);
        }
    }

    private void handleJobStateEvent(JobStateEvent jobEvent) {
        String jobId = jobEvent.getJobId();
        String jobName = jobEvent.getJobName();
        JobStatus status = jobEvent.getJobStatus();
        long eventTime = jobEvent.getCreatedTime();

        switch (status) {
            case FAILED:
                sendAlert("【任务失败】jobId: " + jobId + ", jobName: " + jobName);
                break;
            case FINISHED:
                //sendAlert("任务完成: " + jobId + ", jobName: " + jobName);
                break;
            //其他需要处理的事件
            // case READER_OPEN:
            //     break;
            // case WRITER_CLOSE:
            //     break;
            default:
                log.debug("任务状态变更 | jobId: {}, 状态: {}, 时间: {}",jobId, status, eventTime);
                // 不发送通知
        }
    }

    private void handleAddColumnEvent(AlterTableAddColumnEvent event) {
        String tableName = event.getTableIdentifier().getTableName();
        String columnName = event.getColumn() != null ? event.getColumn().getName() : "未知列";
        sendAlert("【表结构变更】表名: " + tableName + ", 新增列: " + columnName);
    }
    private void handleUpdateColumnEvent(AlterTableColumnsEvent event) {
        String tableName = event.getTableIdentifier().getTableName();
        sendAlert("【表结构变更】表名: " + tableName + ", 更新内容: " + event);
    }
    private void handleDropColumnEvent(AlterTableDropColumnEvent event) {
        String tableName = event.getTableIdentifier().getTableName();
        String columnName = event.getColumn() != null ? event.getColumn() : "未知列";
        sendAlert("【表结构变更】表名: " + tableName + ", 删除列: " + columnName);
    }
    private void handleModifyColumnEvent(AlterTableModifyColumnEvent event) {
        String tableName = event.getTableIdentifier().getTableName();
        String columnName = event.getColumn() != null ? event.getColumn().getName() : "未知列";
        sendAlert("【表结构变更】表名: " + tableName + ", 修改列: " + columnName);
    }

    private void sendAlert(String content) {
        sendWechatMessage(content);
    }

    /**
     * 发送企业微信群消息
     * @param message 消息内容
     */
    void sendWechatMessage(String message) {
        try {
            // 构建完整的Webhook URL（如果配置了secret则添加签名参数）
            String fullUrl = WEBHOOK_URL;
            if (SECRET != null && !SECRET.isEmpty()) {
                long timestamp = System.currentTimeMillis();
                String sign = generateSign(timestamp, SECRET);
                fullUrl = WEBHOOK_URL + "&timestamp=" + timestamp + "&sign=" + sign;
            }
            
            // 转义消息内容中的特殊字符
            String escapedMessage = message.replace("\\", "\\\\")
                                          .replace("\"", "\\\"")
                                          .replace("\n", "\\n")
                                          .replace("\r", "\\r")
                                          .replace("\t", "\\t");
            
            // 构建企业微信消息JSON payload
            String jsonPayload = String.format("{\"msgtype\":\"text\",\"text\":{\"content\":\"%s\"}}", escapedMessage);
            
            // 发送HTTP POST请求
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
                log.info("企业微信消息发送成功: {}", message);
            } else {
                log.error("企业微信消息发送失败，响应码: {}, 消息: {}", responseCode, message);
            }
        } catch (Exception e) {
            log.error("发送企业微信消息异常: {}", message, e);
        }
    }
    
    /**
     * 生成企业 webhook 签名（仅在配置了secret时使用）
     * @param timestamp 时间戳
     * @param secret 密钥
     * @return 签名字符串
     */
    private String generateSign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return URLEncoder.encode(new String(Base64.getEncoder().encode(signData)), "UTF-8");
    }
}