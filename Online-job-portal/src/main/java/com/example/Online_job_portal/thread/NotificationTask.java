package com.example.Online_job_portal.thread;

import com.example.Online_job_portal.dao.NotificationDAO;
import com.example.Online_job_portal.model.Notification;

import java.time.LocalDateTime;

public class NotificationTask implements Runnable {

    private final NotificationDAO notificationDAO;
    private final int userId;
    private final String title;
    private final String message;

    public NotificationTask() {
        this.notificationDAO = null;
        this.userId = 0;
        this.title = "";
        this.message = "Generic Notification";
    }

    public NotificationTask(NotificationDAO notificationDAO, int userId, String title, String message) {
        this.notificationDAO = notificationDAO;
        this.userId = userId;
        this.title = title;
        this.message = message;
    }

    @Override
    public void run() {
        if (notificationDAO != null && userId > 0) {
            Notification notification = new Notification(0, userId, title, message, false, LocalDateTime.now());
            notificationDAO.save(notification);
            System.out.println("Notification queued & saved for user " + userId + ": " + title);
        } else {
            System.out.println("Sending job portal notifications...");
        }
    }
}
