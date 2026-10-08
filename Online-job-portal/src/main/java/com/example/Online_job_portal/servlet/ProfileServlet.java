package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.model.User;
import com.example.Online_job_portal.service.UserService;
import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@WebServlet(name = "ProfileServlet", urlPatterns = {"/servlet/profile"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 2 * 1024 * 1024,      // 2 MB max file size
        maxRequestSize = 5 * 1024 * 1024    // 5 MB max request size
)
public class ProfileServlet extends HttpServlet {

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
        resp.sendRedirect(req.getContextPath() + "/jobseeker/profile");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object userIdObj = session != null ? session.getAttribute("userId") : null;

        if (userIdObj == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=auth_required");
            return;
        }

        int userId = (Integer) userIdObj;
        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String phone = req.getParameter("phone");
        String skills = req.getParameter("skills");

        if (!ValidationUtil.isNotBlank(name)) {
            resp.sendRedirect(req.getContextPath() + "/jobseeker/profile?error=" + encode("Full name is required"));
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            resp.sendRedirect(req.getContextPath() + "/jobseeker/profile?error=" + encode("Valid email is required"));
            return;
        }

        // Fetch existing user to retain resume if none uploaded
        Optional<User> existingUser = userService != null ? userService.findUserById(userId) : Optional.empty();
        String defaultResumePath = existingUser.map(User::getDefaultResumePath).orElse("");

        // Handle optional resume upload
        Part filePart = null;
        try {
            filePart = req.getPart("resumeFile");
        } catch (Exception e) {
            resp.sendRedirect(req.getContextPath() + "/jobseeker/profile?error=" + encode("File exceeds maximum allowed size of 2 MB"));
            return;
        }

        if (filePart != null && filePart.getSize() > 0) {
            if (!ValidationUtil.isValidResumeSize(filePart.getSize())) {
                resp.sendRedirect(req.getContextPath() + "/jobseeker/profile?error=" + encode("File size exceeds 2 MB limit"));
                return;
            }

            String submittedFilename = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            if (!ValidationUtil.isValidResumeExtension(submittedFilename)) {
                resp.sendRedirect(req.getContextPath() + "/jobseeker/profile?error=" + encode("Invalid file format. Only PDF and DOCX files are accepted."));
                return;
            }

            String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "resumes";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String safeFilename = UUID.randomUUID().toString().substring(0, 8) + "_" + submittedFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
            File destFile = new File(dir, safeFilename);
            filePart.write(destFile.getAbsolutePath());

            defaultResumePath = safeFilename;
        }

        if (userService != null) {
            try {
                userService.updateProfile(userId, name, email, phone, skills, defaultResumePath);
            } catch (Exception e) {
                resp.sendRedirect(req.getContextPath() + "/jobseeker/profile?error=" + encode(e.getMessage()));
                return;
            }
        }

        // Update session identity
        session.setAttribute("userName", name.trim());
        session.setAttribute("userEmail", email.trim());

        if (activityLogger != null) {
            activityLogger.log(userId, name, "PROFILE_UPDATE", "Updated applicant profile details and default resume");
        }

        resp.sendRedirect(req.getContextPath() + "/jobseeker/profile?success=" + encode("Profile information and resume updated successfully!"));
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
