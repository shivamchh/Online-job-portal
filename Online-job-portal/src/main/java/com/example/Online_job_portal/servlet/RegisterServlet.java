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

    public RegisterServlet() {
    }

    public RegisterServlet(UserService userService, ActivityLogger activityLogger) {
        this.userService = userService;
        this.activityLogger = activityLogger;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        resolveDependencies();
    }

    private void resolveDependencies() {
        if (userService == null || activityLogger == null) {
            var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            if (ctx != null) {
                if (userService == null) userService = ctx.getBean(UserService.class);
                if (activityLogger == null) activityLogger = ctx.getBean(ActivityLogger.class);
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

        if (!ValidationUtil.isNotBlank(name)) {
            resp.sendRedirect(req.getContextPath() + "/register?error=" + encode("Name is required"));
            return;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            resp.sendRedirect(req.getContextPath() + "/register?error=" + encode("Please enter a valid email address"));
            return;
        }
        if (!ValidationUtil.isValidPassword(password)) {
            resp.sendRedirect(req.getContextPath() + "/register?error=" + encode("Password must be at least 4 characters"));
            return;
        }
        if (!ValidationUtil.isValidRole(role)) {
            role = "JOB_SEEKER";
        }

        if (userService != null && userService.findUserByEmail(email.trim()).isPresent()) {
            resp.sendRedirect(req.getContextPath() + "/register?error=" + encode("An account with this email already exists"));
            return;
        }

        try {
            if (userService != null) {
                User user = userService.registerUser(name.trim(), email.trim(), password, phone, role);
                if (activityLogger != null) {
                    activityLogger.log(user.getId(), user.getName(), "REGISTRATION", "New " + role + " account registered");
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
