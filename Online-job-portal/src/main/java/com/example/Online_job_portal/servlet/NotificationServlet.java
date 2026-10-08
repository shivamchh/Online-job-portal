package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.dao.NotificationDAO;
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

@WebServlet(name = "NotificationServlet", urlPatterns = {"/servlet/notifications"})
public class NotificationServlet extends HttpServlet {

    @Autowired
    private NotificationDAO notificationDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        if (notificationDAO == null) {
            var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            if (ctx != null) {
                notificationDAO = ctx.getBean(NotificationDAO.class);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/jobseeker/dashboard");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object userIdObj = session != null ? session.getAttribute("userId") : null;

        if (userIdObj != null && notificationDAO != null) {
            int userId = (Integer) userIdObj;
            notificationDAO.markAllAsRead(userId);
        }

        resp.sendRedirect(req.getContextPath() + "/jobseeker/dashboard?success=" +
                URLEncoder.encode("All notifications marked as read", StandardCharsets.UTF_8));
    }
}
