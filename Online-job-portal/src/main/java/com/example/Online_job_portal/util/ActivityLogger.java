package com.example.Online_job_portal.util;

import com.example.Online_job_portal.dao.ActivityLogDAO;
import com.example.Online_job_portal.model.ActivityLog;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Multithreaded Activity Logger using a BlockingQueue and a dedicated background consumer thread.
 * Demonstrates thread-safe producer-consumer pattern in Java.
 */
@Component
public class ActivityLogger {

    private final ActivityLogDAO activityLogDAO;
    private final BlockingQueue<ActivityLog> logQueue = new LinkedBlockingQueue<>(1000);
    private volatile boolean running = true;
    private Thread workerThread;

    public ActivityLogger(ActivityLogDAO activityLogDAO) {
        this.activityLogDAO = activityLogDAO;
    }

    @PostConstruct
    public void start() {
        running = true;
        workerThread = new Thread(() -> {
            while (running || !logQueue.isEmpty()) {
                try {
                    ActivityLog log = logQueue.take();
                    activityLogDAO.save(log);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error processing activity log: " + e.getMessage());
                }
            }
        }, "ActivityLogger-Worker");
        workerThread.setDaemon(true);
        workerThread.start();

        // Seed some initial demo logs
        log(1, "Admin User", "SYSTEM_STARTUP", "Application system started successfully");
        log(2, "TechCorp Solutions", "JOB_POST", "Posted job: Senior Java Developer");
        log(3, "Alex Kumar", "APPLICATION", "Applied for Senior Java Developer");
    }

    public void log(int userId, String userName, String action, String details) {
        ActivityLog log = new ActivityLog(0, userId, userName, action, details, LocalDateTime.now());
        logQueue.offer(log);
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    public int getQueueSize() {
        return logQueue.size();
    }
}
