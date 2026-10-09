package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.model.Application;
import com.example.Online_job_portal.model.Job;
import com.example.Online_job_portal.model.User;
import com.example.Online_job_portal.service.ApplicationService;
import com.example.Online_job_portal.service.JobService;
import com.example.Online_job_portal.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servlet providing statistical data in JSON format for Chart.js graphs.
 * Uses only real data: jobs per month for the last 6 months (year-aware),
 * application counts by status, and user counts by role.
 */
@WebServlet(name = "StatisticsServlet", urlPatterns = {"/servlet/statistics", "/api/statistics"})
public class StatisticsServlet extends HttpServlet {

    private static final int MONTHS_SHOWN = 6;
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH);

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserService userService;

    public StatisticsServlet() {
    }

    public StatisticsServlet(JobService jobService, ApplicationService applicationService, UserService userService) {
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.userService = userService;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        if (jobService == null || applicationService == null || userService == null) {
            var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            if (ctx != null) {
                if (jobService == null) jobService = ctx.getBean(JobService.class);
                if (applicationService == null) applicationService = ctx.getBean(ApplicationService.class);
                if (userService == null) userService = ctx.getBean(UserService.class);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        List<Job> allJobs = jobService != null ? jobService.getAllJobs() : Collections.emptyList();
        List<Application> allApplications =
                applicationService != null ? applicationService.getAllApplications() : Collections.emptyList();
        List<User> allUsers = userService != null ? userService.getAllUsers() : Collections.emptyList();

        // 1. Jobs per month for the last N months, built from today's date (no hardcoded months, no fake data)
        Map<YearMonth, Integer> monthCounts = new LinkedHashMap<>();
        YearMonth current = YearMonth.now();
        for (int i = MONTHS_SHOWN - 1; i >= 0; i--) {
            monthCounts.put(current.minusMonths(i), 0);
        }
        for (Job job : allJobs) {
            if (job.getCreatedAt() != null) {
                YearMonth ym = YearMonth.from(job.getCreatedAt());
                if (monthCounts.containsKey(ym)) {
                    monthCounts.merge(ym, 1, Integer::sum);
                }
            }
        }
        Map<String, Integer> jobsByMonth = new LinkedHashMap<>();
        for (Map.Entry<YearMonth, Integer> e : monthCounts.entrySet()) {
            jobsByMonth.put(e.getKey().format(LABEL), e.getValue());
        }

        // 2. Applications by status (dynamically built from ApplicationStatus.values() enum)
        Map<String, Integer> appStatusCounts = new LinkedHashMap<>();
        for (com.example.Online_job_portal.model.enums.ApplicationStatus st : com.example.Online_job_portal.model.enums.ApplicationStatus.values()) {
            appStatusCounts.put(st.getCode(), 0);
        }
        for (Application app : allApplications) {
            String status = app.getStatus() != null ? app.getStatus().toUpperCase() : "SUBMITTED";
            appStatusCounts.merge(status, 1, Integer::sum);
        }

        // 3. Users by role
        Map<String, Integer> roleCounts = new LinkedHashMap<>();
        roleCounts.put("JOB_SEEKER", 0);
        roleCounts.put("EMPLOYER", 0);
        roleCounts.put("ADMIN", 0);
        for (User u : allUsers) {
            String role = (userService != null ? userService.getRole(u) : "JOB_SEEKER").toUpperCase();
            roleCounts.merge(role, 1, Integer::sum);
        }

        // Build JSON
        StringBuilder json = new StringBuilder("{");
        json.append("\"totalJobs\":").append(allJobs.size()).append(",");
        json.append("\"totalApplications\":").append(allApplications.size()).append(",");
        json.append("\"totalUsers\":").append(allUsers.size()).append(",");
        json.append("\"jobsByMonth\":").append(toJsonObject(jobsByMonth)).append(",");
        json.append("\"applicationStatuses\":").append(toJsonObject(appStatusCounts)).append(",");
        json.append("\"roleCounts\":").append(toJsonObject(roleCounts));
        json.append("}");

        PrintWriter out = resp.getWriter();
        out.print(json);
        out.flush();
    }

    /** Converts a map of String to Integer into a JSON object string. */
    private String toJsonObject(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escape(entry.getKey())).append("\":").append(entry.getValue());
            first = false;
        }
        return sb.append("}").toString();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}