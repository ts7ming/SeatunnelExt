package com.ts7ming;

/**
 * Interface for sending notifications
 */
public interface NotificationSender {
    /**
     * Send a notification message
     * @param message The message content to send
     */
    void sendMessage(String message);
}
