package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.service.UserService;
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

@WebServlet(name = "UserServlet", urlPatterns = {"/servlet/users"})
public class UserServlet extends HttpServlet {

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
        resp.sendRedirect(req.getContextPath() + "/admin/users");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object role = session != null ? session.getAttribute("role") : null;
        String adminName = session != null ? (String) session.getAttribute("userName") : "Admin";

        if (session == null || !"ADMIN".equalsIgnoreCase(String.valueOf(role))) {
            resp.sendRedirect(req.getContextPath() + "/access-denied");
            return;
        }

        String idStr = req.getParameter("id");
        String action = req.getParameter("action");

        if (idStr != null && userService != null) {
            try {
                int targetUserId = Integer.parseInt(idStr);
                if ("delete".equalsIgnoreCase(action)) {
                    userService.deleteUser(targetUserId);
                    if (activityLogger != null) {
                        activityLogger.log(0, adminName, "USER_DELETE", "Soft deleted user ID #" + targetUserId);
                    }
                    resp.sendRedirect(req.getContextPath() + "/admin/users?success=" + encode("User deactivated successfully (soft delete applied)."));
                    return;
                } else {
                    userService.toggleUserStatus(targetUserId);
                    if (activityLogger != null) {
                        activityLogger.log(0, adminName, "USER_STATUS_TOGGLE", "Toggled status for user ID #" + targetUserId);
                    }
                    resp.sendRedirect(req.getContextPath() + "/admin/users?success=" + encode("User status updated successfully."));
                    return;
                }
            } catch (Exception e) {
                resp.sendRedirect(req.getContextPath() + "/admin/users?error=" + encode(e.getMessage()));
                return;
            }
        }

        resp.sendRedirect(req.getContextPath() + "/admin/users");
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
