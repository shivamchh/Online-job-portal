package com.example.Online_job_portal.listener;

import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.ThreadManager;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Standard ServletContextListener managing the application lifecycle.
 * Ensures clean and graceful shutdown of background threads, worker pools,
 * and scheduled executors when the web application is stopped.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("JobSphere Application Servlet Context initialized successfully.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("JobSphere Application shutting down: stopping background threads...");
        var ctx = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());
        if (ctx != null) {
            try {
                ThreadManager tm = ctx.getBean(ThreadManager.class);
                tm.shutdown();
                log.info("ThreadManager executor pools terminated cleanly.");
            } catch (Exception e) {
                log.warn("Notice while stopping ThreadManager: {}", e.getMessage());
            }

            try {
                ActivityLogger al = ctx.getBean(ActivityLogger.class);
                al.shutdown();
                log.info("ActivityLogger worker thread stopped cleanly.");
            } catch (Exception e) {
                log.warn("Notice while stopping ActivityLogger: {}", e.getMessage());
            }
        }
    }
}
