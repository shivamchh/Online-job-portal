package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.service.AdminService;
import com.example.Online_job_portal.util.ActivityLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "SettingsServlet", urlPatterns = {"/servlet/settings"})
public class SettingsServlet extends HttpServlet {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ActivityLogger activityLogger;

    @Override
    public void init() throws ServletException {
        super.init();
        if (adminService == null || activityLogger == null) {
            var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            if (ctx != null) {
                if (adminService == null) adminService = ctx.getBean(AdminService.class);
                if (activityLogger == null) activityLogger = ctx.getBean(ActivityLogger.class);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/settings");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object role = session != null ? session.getAttribute("role") : null;
        String userName = session != null ? (String) session.getAttribute("userName") : "Admin";

        if (session == null || !"ADMIN".equalsIgnoreCase(String.valueOf(role))) {
            resp.sendRedirect(req.getContextPath() + "/access-denied");
            return;
        }

        String portalName = req.getParameter("portal_name");
        String maxResumeSize = req.getParameter("max_resume_size_mb");
        String registrationOpen = req.getParameter("registration_open");
        String adminAccessKey = req.getParameter("admin_access_key");
        String requireApproval = req.getParameter("require_account_approval");
        String maintenanceMode = req.getParameter("maintenance_mode");
        String contactEmail = req.getParameter("contact_email");

        if (adminService != null) {
            if (portalName != null && !portalName.isBlank()) {
                adminService.updateSetting("portal_name", portalName.trim());
            }
            if (maxResumeSize != null && !maxResumeSize.isBlank()) {
                adminService.updateSetting("max_resume_size_mb", maxResumeSize.trim());
            }
            if (registrationOpen != null) {
                adminService.updateSetting("registration_open", registrationOpen.trim());
            }
            if (adminAccessKey != null && !adminAccessKey.isBlank()) {
                adminService.updateSetting("admin_access_key", adminAccessKey.trim());
            }
            if (requireApproval != null) {
                adminService.updateSetting("require_account_approval", requireApproval.trim());
            }
            if (maintenanceMode != null) {
                adminService.updateSetting("maintenance_mode", maintenanceMode.trim());
            }
            if (contactEmail != null && !contactEmail.isBlank()) {
                adminService.updateSetting("contact_email", contactEmail.trim());
            }
        }

        if (activityLogger != null) {
            activityLogger.log(0, userName, "SETTINGS_UPDATE", "Updated system configurations and runtime parameters");
        }

        resp.sendRedirect(req.getContextPath() + "/admin/settings?success=" +
                URLEncoder.encode("System configuration settings updated successfully!", StandardCharsets.UTF_8));
    }
}
