package com.example.Online_job_portal.listener;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standard HttpSessionListener that monitors user sessions in real time.
 * Provides active session count for administrative live monitoring.
 */
@WebListener
public class ActiveSessionListener implements HttpSessionListener {

    private static final AtomicInteger ACTIVE_SESSIONS = new AtomicInteger(1);

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        ACTIVE_SESSIONS.incrementAndGet();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        ACTIVE_SESSIONS.updateAndGet(count -> count > 1 ? count - 1 : 1);
    }

    public static int getActiveSessionCount() {
        return Math.max(1, ACTIVE_SESSIONS.get());
    }
}
