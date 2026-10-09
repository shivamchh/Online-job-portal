package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.model.Job;
import com.example.Online_job_portal.service.JobService;
import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet(name = "JobServlet", urlPatterns = {"/servlet/jobs"})
public class JobServlet extends HttpServlet {

    @Autowired
    private JobService jobService;

    @Autowired
    private ActivityLogger activityLogger;

    @Autowired
    private com.example.Online_job_portal.dao.NotificationDAO notificationDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        if (jobService == null || activityLogger == null || notificationDAO == null) {
            var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            if (ctx != null) {
                if (jobService == null) jobService = ctx.getBean(JobService.class);
                if (activityLogger == null) activityLogger = ctx.getBean(ActivityLogger.class);
                if (notificationDAO == null) notificationDAO = ctx.getBean(com.example.Online_job_portal.dao.NotificationDAO.class);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String keyword = req.getParameter("keyword");
        String location = req.getParameter("location");
        String jobType = req.getParameter("jobType");

        List<Job> jobs = jobService != null
                ? jobService.searchJobs(keyword, location, jobType, null, 1, 20)
                : List.of();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < jobs.size(); i++) {
            Job j = jobs.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"id\":%d,\"title\":\"%s\",\"location\":\"%s\",\"jobType\":\"%s\",\"salary\":%.2f}",
                    j.getId(),
                    escapeJson(j.getTitle()),
                    escapeJson(j.getLocation()),
                    escapeJson(j.getJobType()),
                    j.getSalary()));
        }
        sb.append("]");
        out.print(sb.toString());
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object userIdObj = session != null ? session.getAttribute("userId") : null;
        String userName = session != null ? (String) session.getAttribute("userName") : "Employer";
        String role = session != null ? String.valueOf(session.getAttribute("role")) : "";

        if (userIdObj == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=auth_required");
            return;
        }

        int employerId = (Integer) userIdObj;
        String action = req.getParameter("action");

        // Handle Status Toggle (Open <-> Closed)
        if ("toggle".equalsIgnoreCase(action)) {
            String idStr = req.getParameter("id");
            if (idStr != null && jobService != null) {
                int jobId = Integer.parseInt(idStr);
                jobService.toggleJobStatus(jobId);
                if (activityLogger != null) {
                    activityLogger.log(employerId, userName, "JOB_TOGGLE", "Toggled status for job ID #" + jobId);
                }
            }
            String redirectUrl = "ADMIN".equalsIgnoreCase(role) ? "/admin/jobs" : "/employer/my-jobs";
            resp.sendRedirect(req.getContextPath() + redirectUrl + "?success=" + encode("Job status updated successfully."));
            return;
        }

        // Handle Job Approval (Admin Action)
        if ("approve".equalsIgnoreCase(action)) {
            String idStr = req.getParameter("id");
            if (idStr != null && jobService != null) {
                int jobId = Integer.parseInt(idStr);
                jobService.setJobStatus(jobId, "APPROVED");
                if (activityLogger != null) {
                    activityLogger.log(employerId, userName, "JOB_APPROVE", "Approved job ID #" + jobId + " (Status: APPROVED)");
                }
                if (notificationDAO != null) {
                    jobService.getJobById(jobId).ifPresent(j -> notificationDAO.save(new com.example.Online_job_portal.model.Notification(
                            j.getEmployerId(),
                            "🎉 Job #" + jobId + " Approved!",
                            "Your job posting '" + j.getTitle() + "' has been approved by the platform administrator and is now live.",
                            false)));
                }
                resp.sendRedirect(req.getContextPath() + "/admin/jobs?success=" + encode("🎉 Congratulations! Job #" + jobId + " has been SELECTED & APPROVED successfully! The job listing is now live and published for all candidates to apply."));
                return;
            }
        }

        // Handle Job Rejection (Admin Action)
        if ("reject".equalsIgnoreCase(action)) {
            String idStr = req.getParameter("id");
            if (idStr != null && jobService != null) {
                int jobId = Integer.parseInt(idStr);
                jobService.setJobStatus(jobId, "REJECTED");
                if (activityLogger != null) {
                    activityLogger.log(employerId, userName, "JOB_REJECT", "Rejected job ID #" + jobId + " (Status: REJECTED)");
                }
                if (notificationDAO != null) {
                    jobService.getJobById(jobId).ifPresent(j -> notificationDAO.save(new com.example.Online_job_portal.model.Notification(
                            j.getEmployerId(),
                            "❌ Job #" + jobId + " Rejected",
                            "Your job listing '" + j.getTitle() + "' was reviewed and marked as REJECTED.",
                            false)));
                }
                resp.sendRedirect(req.getContextPath() + "/admin/jobs?rejected=true&jobId=" + jobId + "&message=" + encode("Job #" + jobId + " status has been set to REJECTED. The job listing is closed."));
                return;
            }
        }

        // Handle Soft Delete
        if ("delete".equalsIgnoreCase(action)) {
            String idStr = req.getParameter("id");
            if (idStr != null && jobService != null) {
                int jobId = Integer.parseInt(idStr);
                jobService.deleteJob(jobId);
                if (activityLogger != null) {
                    activityLogger.log(employerId, userName, "JOB_DELETE", "Soft deleted job ID #" + jobId);
                }
            }
            String redirectUrl = "ADMIN".equalsIgnoreCase(role) ? "/admin/jobs" : "/employer/my-jobs";
            resp.sendRedirect(req.getContextPath() + redirectUrl + "?success=" + encode("Job marked as inactive (soft delete applied)."));
            return;
        }

        // Handle Job Update (Edit Details)
        if ("update".equalsIgnoreCase(action)) {
            String idStr = req.getParameter("id");
            String title = req.getParameter("title");
            String description = req.getParameter("description");
            String requirements = req.getParameter("requirements");
            String salaryStr = req.getParameter("salary");
            String location = req.getParameter("location");
            String jobType = req.getParameter("jobType");

            if (idStr != null && ValidationUtil.isNotBlank(title) && ValidationUtil.isNotBlank(description) && jobService != null) {
                int jobId = Integer.parseInt(idStr);
                double salary = 0.0;
                try {
                    salary = Double.parseDouble(salaryStr);
                } catch (Exception ignored) {}

                jobService.updateJobDetails(jobId, title.trim(), description.trim(),
                        requirements != null ? requirements.trim() : "",
                        salary,
                        location != null ? location.trim() : "Remote",
                        jobType != null ? jobType.trim() : "Full Time");

                if (activityLogger != null) {
                    activityLogger.log(employerId, userName, "JOB_UPDATE", "Updated job details for #" + jobId);
                }
                String redirectUrl = "ADMIN".equalsIgnoreCase(role) ? "/admin/jobs" : "/employer/my-jobs";
                resp.sendRedirect(req.getContextPath() + redirectUrl + "?success=" + encode("Job #" + jobId + " updated successfully!"));
                return;
            }
        }

        // Handle Job Creation
        String title = req.getParameter("title");
        String description = req.getParameter("description");
        String requirements = req.getParameter("requirements");
        String salaryStr = req.getParameter("salary");
        String location = req.getParameter("location");
        String jobType = req.getParameter("jobType");

        if (!ValidationUtil.isNotBlank(title) || !ValidationUtil.isNotBlank(description)) {
            resp.sendRedirect(req.getContextPath() + "/employer/post-job?error=" + encode("Title and Description are required"));
            return;
        }

        double salary = 0.0;
        try {
            salary = Double.parseDouble(salaryStr);
        } catch (Exception ignored) {
        }

        Job job = new Job(0, employerId, title.trim(), description.trim(),
                requirements != null ? requirements.trim() : "",
                salary,
                location != null ? location.trim() : "Remote",
                jobType != null ? jobType.trim() : "Full Time",
                "OPEN");

        if (jobService != null) {
            jobService.postJob(job);
        }
        if (activityLogger != null) {
            activityLogger.log(employerId, userName, "JOB_POST", "Posted new job: " + title);
        }

        resp.sendRedirect(req.getContextPath() + "/employer/my-jobs?success=" + encode("Job posted successfully!"));
    }

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
