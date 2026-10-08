package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.model.User;
import com.example.Online_job_portal.service.UserService;
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
import java.util.Optional;

@WebServlet(name = "LoginServlet", urlPatterns = {"/servlet/login"})
public class LoginServlet extends HttpServlet {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityLogger activityLogger;

    @Override
    public void init() throws ServletException {
        super.init();
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
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (!ValidationUtil.isValidEmail(email) || !ValidationUtil.isNotBlank(password)) {
            resp.sendRedirect(req.getContextPath() + "/login?error=invalid_input");
            return;
        }

        Optional<User> loggedInUser = userService != null ? userService.login(email.trim(), password) : Optional.empty();

        if (loggedInUser.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/login?error=invalid_credentials");
            return;
        }

        User user = loggedInUser.get();
        String role = userService.getRole(user);

        HttpSession session = req.getSession(true);
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
    }
}
