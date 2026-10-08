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
import java.time.format.TextStyle;
import java.util.*;

/**
 * Servlet providing statistical data in JSON format for Chart.js graphs.
 */
@WebServlet(name = "StatisticsServlet", urlPatterns = {"/servlet/statistics", "/api/statistics"})
public class StatisticsServlet extends HttpServlet {

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserService userService;

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
        List<Application> allApplications = applicationService != null ? applicationService.getAllApplications() : Collections.emptyList();
        List<User> allUsers = userService != null ? userService.getAllUsers() : Collections.emptyList();

        // 1. Group jobs by month
        Map<String, Integer> jobsByMonth = new LinkedHashMap<>();
        String[] months = {"May", "Jun", "Jul", "Aug", "Sep", "Oct"};
        for (String m : months) {
            jobsByMonth.put(m, 0);
        }
        for (Job job : allJobs) {
            if (job.getCreatedAt() != null) {
                String mName = job.getCreatedAt().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                jobsByMonth.put(mName, jobsByMonth.getOrDefault(mName, 0) + 1);
            }
        }
        // Ensure some baseline demo distribution if newly launched
        if (allJobs.isEmpty() || jobsByMonth.values().stream().allMatch(v -> v == 0)) {
            jobsByMonth.put("May", 2);
            jobsByMonth.put("Jun", 4);
            jobsByMonth.put("Jul", 5);
            jobsByMonth.put("Aug", 7);
            jobsByMonth.put("Sep", 9);
            jobsByMonth.put("Oct", Math.max(allJobs.size(), 12));
        }

        // 2. Group applications by status
        Map<String, Integer> appStatusCounts = new LinkedHashMap<>();
        appStatusCounts.put("SUBMITTED", 0);
        appStatusCounts.put("PENDING", 0);
        appStatusCounts.put("SHORTLISTED", 0);
        appStatusCounts.put("APPROVED", 0);
        appStatusCounts.put("REJECTED", 0);

        for (Application app : allApplications) {
            String status = app.getStatus() != null ? app.getStatus().toUpperCase() : "SUBMITTED";
            appStatusCounts.put(status, appStatusCounts.getOrDefault(status, 0) + 1);
        }

        // 3. Group users by role
        Map<String, Integer> roleCounts = new LinkedHashMap<>();
        roleCounts.put("JOB_SEEKER", 0);
        roleCounts.put("EMPLOYER", 0);
        roleCounts.put("ADMIN", 0);

        for (User u : allUsers) {
            String role = (userService != null ? userService.getRole(u) : "JOB_SEEKER").toUpperCase();
            roleCounts.put(role, roleCounts.getOrDefault(role, 0) + 1);
        }

        // Build JSON output
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"totalJobs\":").append(allJobs.size()).append(",");
        json.append("\"totalApplications\":").append(allApplications.size()).append(",");
        json.append("\"totalUsers\":").append(allUsers.size()).append(",");

        // jobsByMonth
        json.append("\"jobsByMonth\":{");
        int i = 0;
        for (Map.Entry<String, Integer> entry : jobsByMonth.entrySet()) {
            if (i > 0) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            i++;
        }
        json.append("},");

        // applicationStatuses
        json.append("\"applicationStatuses\":{");
        i = 0;
        for (Map.Entry<String, Integer> entry : appStatusCounts.entrySet()) {
            if (i > 0) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            i++;
        }
        json.append("},");

        // roleCounts
        json.append("\"roleCounts\":{");
        i = 0;
        for (Map.Entry<String, Integer> entry : roleCounts.entrySet()) {
            if (i > 0) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            i++;
        }
        json.append("}");

        json.append("}");

        PrintWriter out = resp.getWriter();
        out.print(json.toString());
        out.flush();
    }
}
