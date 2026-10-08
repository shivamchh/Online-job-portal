package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.model.Message;
import com.example.Online_job_portal.service.MessageService;
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

/**
 * Servlet handling candidate & employer direct messaging.
 */
@WebServlet(name = "MessageServlet", urlPatterns = {"/servlet/messages"})
public class MessageServlet extends HttpServlet {

    @Autowired
    private MessageService messageService;

    @Override
    public void init() throws ServletException {
        super.init();
        if (messageService == null) {
            var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            if (ctx != null) {
                messageService = ctx.getBean(MessageService.class);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object userIdObj = session != null ? session.getAttribute("userId") : null;

        if (userIdObj == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please sign in");
            return;
        }

        int currentUserId = (Integer) userIdObj;
        String otherUserStr = req.getParameter("otherUserId");

        List<Message> messages;
        if (otherUserStr != null && !otherUserStr.isBlank() && messageService != null) {
            int otherUserId = Integer.parseInt(otherUserStr);
            messages = messageService.getConversation(currentUserId, otherUserId);
        } else if (messageService != null) {
            messages = messageService.getUserMessages(currentUserId);
        } else {
            messages = List.of();
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"id\":%d,\"senderId\":%d,\"receiverId\":%d,\"content\":\"%s\",\"sentAt\":\"%s\"}",
                    m.getId(), m.getSenderId(), m.getReceiverId(), escapeJson(m.getContent()), m.getSentAt()));
        }
        sb.append("]");
        out.print(sb.toString());
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object userIdObj = session != null ? session.getAttribute("userId") : null;
        String senderName = session != null ? (String) session.getAttribute("userName") : "User";
        String role = session != null ? (String) session.getAttribute("role") : "";

        if (userIdObj == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=auth_required");
            return;
        }

        int senderId = (Integer) userIdObj;
        String receiverIdStr = req.getParameter("receiverId");
        String content = req.getParameter("content");
        String returnUrl = req.getParameter("returnUrl");

        if (!ValidationUtil.isNotBlank(receiverIdStr) || !ValidationUtil.isNotBlank(content)) {
            String fallback = "EMPLOYER".equals(role) ? "/employer/messages" : "/jobseeker/messages";
            resp.sendRedirect(req.getContextPath() + (returnUrl != null ? returnUrl : fallback) + "?error=" + encode("Receiver and message content cannot be empty"));
            return;
        }

        try {
            int receiverId = Integer.parseInt(receiverIdStr);
            if (messageService != null) {
                messageService.sendMessage(senderId, receiverId, content, senderName);
            }

            String defaultRedirect = "EMPLOYER".equals(role)
                    ? "/employer/messages?withUser=" + receiverId
                    : "/jobseeker/messages?withUser=" + receiverId;

            String target = (returnUrl != null && !returnUrl.isBlank()) ? returnUrl : defaultRedirect;
            resp.sendRedirect(req.getContextPath() + target + (target.contains("?") ? "&" : "?") + "success=" + encode("Message sent successfully!"));
        } catch (Exception e) {
            String fallback = "EMPLOYER".equals(role) ? "/employer/messages" : "/jobseeker/messages";
            resp.sendRedirect(req.getContextPath() + fallback + "?error=" + encode(e.getMessage()));
        }
    }

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
