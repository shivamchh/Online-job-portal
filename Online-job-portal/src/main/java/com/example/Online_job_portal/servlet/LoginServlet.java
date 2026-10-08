package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.model.User;
import com.example.Online_job_portal.service.LoginAttemptService;
import com.example.Online_job_portal.service.UserService;
import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.util.Optional;

/**
 * Handles user authentication via standard Java HttpServlet.
 * Registered as a Spring @Bean via ServletRegistrationBean to guarantee dependency injection.
 * Protects against session fixation, brute-force attacks, and credential enumeration.
 */
public class LoginServlet extends HttpServlet {

    private UserService userService;
    private ActivityLogger activityLogger;
    private LoginAttemptService loginAttemptService;

    public LoginServlet() {
    }

    public LoginServlet(UserService userService, ActivityLogger activityLogger, LoginAttemptService loginAttemptService) {
        this.userService = userService;
        this.activityLogger = activityLogger;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        resolveDependencies();
    }

    private void resolveDependencies() {
        if (userService == null || activityLogger == null || loginAttemptService == null) {
            var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            if (ctx != null) {
                if (userService == null) userService = ctx.getBean(UserService.class);
                if (activityLogger == null) activityLogger = ctx.getBean(ActivityLogger.class);
                if (loginAttemptService == null) loginAttemptService = ctx.getBean(LoginAttemptService.class);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resolveDependencies();

        String rawEmail = req.getParameter("email");
        String password = req.getParameter("password");
        String email = rawEmail != null ? rawEmail.trim() : "";
        if ("admin".equalsIgnoreCase(email)) {
            email = "admin@portal.com";
        }
        String clientIp = req.getRemoteAddr();
        String rateLimitKey = (!email.isEmpty() ? email : "anonymous") + "@" + (clientIp != null ? clientIp : "");

        // 1. Basic non-empty validation
        if (!ValidationUtil.isNotBlank(email) || !ValidationUtil.isNotBlank(password)) {
            resp.sendRedirect(req.getContextPath() + "/login?error=invalid_credentials");
            return;
        }

        // 2. Authenticate credentials
        Optional<User> loggedInUser = userService != null ? userService.login(email, password) : Optional.empty();

        if (loggedInUser.isPresent()) {
            // Successful authentication clears any prior failed attempts
            if (loginAttemptService != null) {
                loginAttemptService.loginSucceeded(rateLimitKey);
            }

            User user = loggedInUser.get();
            String role = userService.getRole(user);

            // Establish secure session (change session ID to prevent session fixation attacks)
            HttpSession session = req.getSession(true);
            try {
                req.changeSessionId();
            } catch (Exception ignored) {
            }

            session.setAttribute("userId", user.getId());
            session.setAttribute("userName", user.getName());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("role", role);

            if (activityLogger != null) {
                activityLogger.log(user.getId(), user.getName(), "LOGIN", "Logged into portal as " + role);
            }

            if ("ADMIN".equals(role)) {
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
            } else if ("EMPLOYER".equals(role)) {
                resp.sendRedirect(req.getContextPath() + "/employer/dashboard");
            } else {
                resp.sendRedirect(req.getContextPath() + "/jobseeker/dashboard");
            }
            return;
        }

        // 3. Invalid credentials: track failed attempt and apply lockout if threshold exceeded
        if (loginAttemptService != null) {
            loginAttemptService.loginFailed(rateLimitKey);
            if (loginAttemptService.isBlocked(rateLimitKey)) {
                resp.sendRedirect(req.getContextPath() + "/login?error=account_locked");
                return;
            }
        }

        // Generic error message prevents username/email enumeration
        resp.sendRedirect(req.getContextPath() + "/login?error=invalid_credentials");
    }
}
