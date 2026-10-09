package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.model.Application;
import com.example.Online_job_portal.service.ApplicationService;
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

@WebServlet(name = "ApplicationServlet", urlPatterns = {"/servlet/applications"})
public class ApplicationServlet extends HttpServlet {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private com.example.Online_job_portal.service.JobService jobService;

    @Override
    public void init() throws ServletException {
        super.init();
        var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (ctx != null) {
            if (applicationService == null) {
                applicationService = ctx.getBean(ApplicationService.class);
            }
            if (jobService == null) {
                jobService = ctx.getBean(com.example.Online_job_portal.service.JobService.class);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Application> list = applicationService != null ? applicationService.getAllApplications() : List.of();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Application a = list.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"id\":%d,\"jobId\":%d,\"jobSeekerId\":%d,\"status\":\"%s\",\"appliedDate\":\"%s\"}",
                    a.getId(), a.getJobId(), a.getJobSeekerId(), a.getStatus(), a.getAppliedDate()));
        }
        sb.append("]");
        out.print(sb.toString());
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=auth_required");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        String reviewerName = (String) session.getAttribute("userName");

        String action = req.getParameter("action");
        if ("updateStatus".equalsIgnoreCase(action)) {
            String appIdStr = req.getParameter("applicationId");
            String newStatus = req.getParameter("status");
            String notes = req.getParameter("notes");

            if (appIdStr != null && newStatus != null && applicationService != null) {
                try {
                    int appId = Integer.parseInt(appIdStr);
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        applicationService.updateApplicationStatus(appId, newStatus, notes, reviewerName);
                    } else if ("EMPLOYER".equalsIgnoreCase(role) && jobService != null) {
                        applicationService.updateApplicationStatusByEmployer(appId, userId, newStatus, notes, reviewerName, jobService);
                    } else {
                        resp.sendRedirect(req.getContextPath() + "/access-denied");
                        return;
                    }
                    if ("APPROVED".equalsIgnoreCase(newStatus) || "SHORTLISTED".equalsIgnoreCase(newStatus)) {
                        resp.sendRedirect(req.getContextPath() + "/employer/applications?success=" + encode("🎉 Congratulations! Candidate Application #" + appId + " has been SELECTED (" + newStatus + ") successfully!"));
                    } else if ("REJECTED".equalsIgnoreCase(newStatus)) {
                        resp.sendRedirect(req.getContextPath() + "/employer/applications?success=" + encode("Candidate Application #" + appId + " status marked as REJECTED."));
                    } else {
                        resp.sendRedirect(req.getContextPath() + "/employer/applications?success=" + encode("Application #" + appId + " updated to " + newStatus));
                    }
                    return;
                } catch (SecurityException e) {
                    resp.sendRedirect(req.getContextPath() + "/employer/applications?error=" + encode("Access Denied: You cannot update another employer's application"));
                    return;
                } catch (Exception e) {
                    resp.sendRedirect(req.getContextPath() + "/employer/applications?error=" + encode(e.getMessage()));
                    return;
                }
            }
        }

        if ("withdraw".equalsIgnoreCase(action)) {
            String appIdStr = req.getParameter("applicationId");
            if (appIdStr != null && applicationService != null) {
                try {
                    int appId = Integer.parseInt(appIdStr);
                    applicationService.withdrawApplication(appId, userId);
                    resp.sendRedirect(req.getContextPath() + "/jobseeker/applications?success=" + encode("Application #" + appId + " has been withdrawn successfully."));
                    return;
                } catch (Exception e) {
                    resp.sendRedirect(req.getContextPath() + "/jobseeker/applications?error=" + encode(e.getMessage()));
                    return;
                }
            }
        }

        resp.sendRedirect(req.getContextPath() + "/employer/applications");
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
