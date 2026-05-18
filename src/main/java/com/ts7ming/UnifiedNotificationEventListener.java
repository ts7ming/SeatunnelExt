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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UnifiedNotificationEventListener implements EventHandler {
    /**
     * Map to store notification sender for each job.
     * Key: jobId, Value: NotificationSender instance for that job
     * This allows different jobs to use different notification configurations.
     */
    private static final Map<String, NotificationSender> jobSenders = new ConcurrentHashMap<>();
    
    /**
     * Map to track which jobs have been initialized.
     * Key: jobId, Value: Boolean indicating if initialized
     */
    private static final Map<String, Boolean> jobInitialized = new ConcurrentHashMap<>();

    /**
     * Get or create notification sender for a specific job.
     * Each job can have its own configuration based on ext.config.path system property.
     * 
     * @param jobId The job identifier
     * @return NotificationSender for this job, or null if not configured
     */
    private NotificationSender getOrCreateSenderForJob(String jobId) {
        // Check if already initialized for this job
        if (jobInitialized.getOrDefault(jobId, false)) {
            return jobSenders.get(jobId);
        }
        
        // Synchronize per job to avoid duplicate initialization
        synchronized (getJobLock(jobId)) {
            // Double-check after acquiring lock
            if (jobInitialized.getOrDefault(jobId, false)) {
                return jobSenders.get(jobId);
            }
            
            String configPath = System.getProperty("ext.config.path");
            NotificationSender sender = null;
            
            if (configPath != null && !configPath.isEmpty()) {
                try {
                    log.info("Loading notification configuration for job {} from: {}", jobId, configPath);
                    NotificationConfig config = NotificationConfig.loadFromPath(configPath);
                    sender = NotificationSenderFactory.createSender(config);
                    log.info("Notification sender initialized for job {}: type={}", jobId, config.getNotificationType());
                } catch (Exception e) {
                    log.error("Failed to initialize notification sender for job {} from config: {}", jobId, configPath, e);
                    sender = null;
                }
            } else {
                log.debug("ext.config.path is not set for job {}, notification will be disabled", jobId);
            }
            
            // Store the sender (may be null if not configured)
            jobSenders.put(jobId, sender);
            jobInitialized.put(jobId, true);
            
            return sender;
        }
    }
    
    /**
     * Get a lock object for synchronizing initialization of a specific job.
     * Uses jobId as the lock key to allow concurrent initialization of different jobs.
     */
    private Object getJobLock(String jobId) {
        // Use intern() to ensure we get the same String object for the same jobId
        return jobId.intern();
    }

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
            log.debug("Ignoring unhandled event type: {}", eventType);
        }
    }

    private void handleJobStateEvent(JobStateEvent jobEvent) {
        String jobId = jobEvent.getJobId();
        String jobName = jobEvent.getJobName();
        JobStatus status = jobEvent.getJobStatus();
        long eventTime = jobEvent.getCreatedTime();

        switch (status) {
            case FAILED:
                sendAlert(jobId, "【Task Failed】jobId: " + jobId + ", jobName: " + jobName);
                break;
            case FINISHED:
                //sendAlert(jobId, "Task Completed: " + jobId + ", jobName: " + jobName);
                break;
            // Other events to handle
            // case READER_OPEN:
            //     break;
            // case WRITER_CLOSE:
            //     break;
            default:
                log.debug("Job status change | jobId: {}, status: {}, time: {}", jobId, status, eventTime);
                // Don't send notification
        }
    }

    private void handleAddColumnEvent(AlterTableAddColumnEvent event) {
        String tableName = event.getTableIdentifier().getTableName();
        String columnName = event.getColumn() != null ? event.getColumn().getName() : "Unknown column";
        // Note: Schema change events may not have jobId, using table name as identifier
        sendAlert(tableName, "【Schema Change】Table: " + tableName + ", Added column: " + columnName);
    }
    
    private void handleUpdateColumnEvent(AlterTableColumnsEvent event) {
        String tableName = event.getTableIdentifier().getTableName();
        sendAlert(tableName, "【Schema Change】Table: " + tableName + ", Update content: " + event);
    }
    
    private void handleDropColumnEvent(AlterTableDropColumnEvent event) {
        String tableName = event.getTableIdentifier().getTableName();
        String columnName = event.getColumn() != null ? event.getColumn() : "Unknown column";
        sendAlert(tableName, "【Schema Change】Table: " + tableName + ", Dropped column: " + columnName);
    }
    
    private void handleModifyColumnEvent(AlterTableModifyColumnEvent event) {
        String tableName = event.getTableIdentifier().getTableName();
        String columnName = event.getColumn() != null ? event.getColumn().getName() : "Unknown column";
        sendAlert(tableName, "【Schema Change】Table: " + tableName + ", Modified column: " + columnName);
    }

    /**
     * Send alert using the notification sender specific to this job.
     * Each job maintains its own notification configuration.
     * 
     * @param jobIdentifier Job ID or other identifier
     * @param content The message content to send
     */
    private void sendAlert(String jobIdentifier, String content) {
        // Get or create sender for this specific job
        NotificationSender sender = getOrCreateSenderForJob(jobIdentifier);
        
        if (sender != null) {
            sender.sendMessage(content);
        } else {
            log.debug("Notification sender not available for job {}, skipping message: {}", jobIdentifier, content);
        }
    }
}
