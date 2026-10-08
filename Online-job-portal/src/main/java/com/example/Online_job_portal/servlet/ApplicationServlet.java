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

    @Override
    public void init() throws ServletException {
        super.init();
        if (applicationService == null) {
            var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            if (ctx != null) {
                applicationService = ctx.getBean(ApplicationService.class);
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
        String reviewerName = session != null ? (String) session.getAttribute("userName") : "Employer";

        String action = req.getParameter("action");
        if ("updateStatus".equalsIgnoreCase(action)) {
            String appIdStr = req.getParameter("applicationId");
            String newStatus = req.getParameter("status");
            String notes = req.getParameter("notes");

            if (appIdStr != null && newStatus != null && applicationService != null) {
                try {
                    int appId = Integer.parseInt(appIdStr);
                    applicationService.updateApplicationStatus(appId, newStatus, notes, reviewerName);
                    resp.sendRedirect(req.getContextPath() + "/employer/applications?success=" + encode("Application #" + appId + " updated to " + newStatus));
                    return;
                } catch (Exception e) {
                    resp.sendRedirect(req.getContextPath() + "/employer/applications?error=" + encode(e.getMessage()));
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
