package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.model.User;
import com.example.Online_job_portal.service.UserService;
import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles new account registration via standard Java HttpServlet.
 * Registered as a Spring @Bean via ServletRegistrationBean to guarantee dependency injection.
 */
public class RegisterServlet extends HttpServlet {

    private UserService userService;
    private ActivityLogger activityLogger;
    private com.example.Online_job_portal.dao.SettingsDAO settingsDAO;

    public RegisterServlet() {
    }

    public RegisterServlet(UserService userService, ActivityLogger activityLogger) {
        this.userService = userService;
        this.activityLogger = activityLogger;
    }

    public RegisterServlet(UserService userService, ActivityLogger activityLogger, com.example.Online_job_portal.dao.SettingsDAO settingsDAO) {
        this.userService = userService;
        this.activityLogger = activityLogger;
        this.settingsDAO = settingsDAO;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        resolveDependencies();
    }

    private void resolveDependencies() {
        if (userService == null || activityLogger == null || settingsDAO == null) {
            var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            if (ctx != null) {
                if (userService == null) userService = ctx.getBean(UserService.class);
                if (activityLogger == null) activityLogger = ctx.getBean(ActivityLogger.class);
                if (settingsDAO == null) {
                    try {
                        settingsDAO = ctx.getBean(com.example.Online_job_portal.dao.SettingsDAO.class);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/register");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resolveDependencies();

        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String phone = req.getParameter("phone");
        String role = req.getParameter("role");
        String adminAccessKey = req.getParameter("adminAccessKey");

        if (!ValidationUtil.isNotBlank(name)) {
            resp.sendRedirect(req.getContextPath() + "/register?error=" + encode("Name is required"));
            return;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            resp.sendRedirect(req.getContextPath() + "/register?error=" + encode("Please enter a valid email address (e.g. name@domain.com)"));
            return;
        }
        if (!ValidationUtil.isValidPassword(password)) {
            resp.sendRedirect(req.getContextPath() + "/register?error=" + encode("Password must be at least 4 characters"));
            return;
        }
        if (!ValidationUtil.isValidRole(role)) {
            role = "JOB_SEEKER";
        }

        // 1. Check if public registration is enabled
        String registrationOpen = settingsDAO != null ? settingsDAO.getValue("registration_open", "true") : "true";
        if (!"true".equalsIgnoreCase(registrationOpen) && !"ADMIN".equalsIgnoreCase(role)) {
            resp.sendRedirect(req.getContextPath() + "/register?error=" + encode("Public registration is currently disabled by the portal administrator. Please contact the owner for access."));
            return;
        }

        // 2. Strict owner access restriction for ADMIN role
        if ("ADMIN".equalsIgnoreCase(role)) {
            String configuredAdminKey = settingsDAO != null ? settingsDAO.getValue("admin_access_key", "ADMIN@ACCESS@2026") : "ADMIN@ACCESS@2026";
            if (adminAccessKey == null || !adminAccessKey.trim().equals(configuredAdminKey.trim())) {
                resp.sendRedirect(req.getContextPath() + "/register?error=" + encode("Admin registration is restricted. A valid Owner Authorization Key granted by the owner is required."));
                return;
            }
        }

        // 3. Prevent duplicate email registrations
        if (userService != null && userService.findUserByEmail(email.trim()).isPresent()) {
            resp.sendRedirect(req.getContextPath() + "/register?error=" + encode("An account with this email already exists"));
            return;
        }

        try {
            if (userService != null) {
                // 4. Check if account approval is required
                String requireApproval = settingsDAO != null ? settingsDAO.getValue("require_account_approval", "false") : "false";
                String initialStatus = ("true".equalsIgnoreCase(requireApproval) && !"ADMIN".equalsIgnoreCase(role)) ? "PENDING" : "ACTIVE";

                User user = userService.registerUser(name.trim(), email.trim(), password, phone, role, initialStatus);
                if (activityLogger != null) {
                    activityLogger.log(user.getId(), user.getName(), "REGISTRATION", "New " + role + " account registered (Status: " + initialStatus + ")");
                }

                if ("PENDING".equalsIgnoreCase(initialStatus)) {
                    resp.sendRedirect(req.getContextPath() + "/login?pending_approval=true");
                    return;
                }
            }
            resp.sendRedirect(req.getContextPath() + "/login?registered=success");
        } catch (Exception e) {
            resp.sendRedirect(req.getContextPath() + "/register?error=" + encode(e.getMessage()));
        }
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
