package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.listener.ActiveSessionListener;
import com.example.Online_job_portal.dao.ActivityLogDAO;
import com.example.Online_job_portal.model.ActivityLog;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Real-time Activity Log Servlet.
 * Streams JSON logs and live active session count for automatic dashboard auto-refresh.
 */
@WebServlet(name = "ActivityLogServlet", urlPatterns = {"/servlet/activities", "/api/activities"})
public class ActivityLogServlet extends HttpServlet {

    private ActivityLogDAO activityLogDAO;

    public ActivityLogServlet() {
    }

    public ActivityLogServlet(ActivityLogDAO activityLogDAO) {
        this.activityLogDAO = activityLogDAO;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        if (activityLogDAO == null) {
            var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            if (ctx != null) {
                this.activityLogDAO = ctx.getBean(ActivityLogDAO.class);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        List<ActivityLog> logs = activityLogDAO != null ? activityLogDAO.findLatest(15) : List.of();
        int activeSessions = ActiveSessionListener.getActiveSessionCount();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"activeSessions\":").append(activeSessions).append(",\"logs\":[");
        for (int i = 0; i < logs.size(); i++) {
            ActivityLog l = logs.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"id\":%d,\"userName\":\"%s\",\"action\":\"%s\",\"details\":\"%s\",\"timestamp\":\"%s\"}",
                    l.getId(),
                    escape(l.getUserName()),
                    escape(l.getAction()),
                    escape(l.getDetails()),
                    l.getTimestamp() != null ? l.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : ""));
        }
        sb.append("]}");

        PrintWriter out = resp.getWriter();
        out.print(sb);
        out.flush();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
