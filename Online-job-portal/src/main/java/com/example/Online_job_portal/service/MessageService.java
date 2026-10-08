package com.example.Online_job_portal.service;

import com.example.Online_job_portal.dao.MessageDAO;
import com.example.Online_job_portal.dao.NotificationDAO;
import com.example.Online_job_portal.model.Message;
import com.example.Online_job_portal.thread.NotificationTask;
import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.ThreadManager;
import com.example.Online_job_portal.util.ValidationUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private final MessageDAO messageDAO;
    private final NotificationDAO notificationDAO;
    private final ThreadManager threadManager;
    private final ActivityLogger activityLogger;

    public MessageService(MessageDAO messageDAO,
                          NotificationDAO notificationDAO,
                          ThreadManager threadManager,
                          ActivityLogger activityLogger) {
        this.messageDAO = messageDAO;
        this.notificationDAO = notificationDAO;
        this.threadManager = threadManager;
        this.activityLogger = activityLogger;
    }

    public Message sendMessage(int senderId, int receiverId, String content, String senderName) {
        if (!ValidationUtil.isNotBlank(content)) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        Message message = new Message(0, senderId, receiverId,
                senderName != null ? senderName : "User #" + senderId,
                "", content.trim(), LocalDateTime.now(), false);

        messageDAO.save(message);

        // Multithreaded activity logging
        if (activityLogger != null) {
            activityLogger.log(senderId, senderName, "MESSAGE_SENT",
                    "Sent direct message to user ID #" + receiverId);
        }

        // Asynchronous notification delivery to recipient
        if (threadManager != null && notificationDAO != null) {
            threadManager.executeTask(new NotificationTask(
                    notificationDAO,
                    receiverId,
                    "New Message Received",
                    "You received a new message from " + (senderName != null ? senderName : "a user") + ": \""
                            + (content.length() > 40 ? content.substring(0, 37) + "..." : content) + "\""
            ));
        }

        return message;
    }

    public List<Message> getConversation(int user1Id, int user2Id) {
        return messageDAO.findConversation(user1Id, user2Id);
    }

    public List<Message> getUserMessages(int userId) {
        return messageDAO.findByUser(userId);
    }

    public int getUnreadCount(int userId) {
        return messageDAO.getUnreadCount(userId);
    }

    public void markAsRead(int messageId) {
        messageDAO.markAsRead(messageId);
    }

    public void markConversationAsRead(int currentUserId, int otherUserId) {
        List<Message> conversation = messageDAO.findConversation(currentUserId, otherUserId);
        for (Message m : conversation) {
            if (m.getReceiverId() == currentUserId && !m.isRead()) {
                messageDAO.markAsRead(m.getId());
            }
        }
    }
}
