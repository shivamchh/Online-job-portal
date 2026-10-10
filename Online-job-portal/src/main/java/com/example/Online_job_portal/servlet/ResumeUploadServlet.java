package com.example.Online_job_portal.servlet;

import com.example.Online_job_portal.exception.ApplicationException;
import com.example.Online_job_portal.service.ApplicationService;
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
import java.util.UUID;

/**
 * Servlet handling resume file uploads using @MultipartConfig.
 * Enforces PDF/DOCX formats and 2 MB maximum size limit.
 */
@WebServlet(name = "ResumeUploadServlet", urlPatterns = {"/servlet/resume-upload", "/applications/upload-resume"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 2 * 1024 * 1024,      // 2 MB max file size
        maxRequestSize = 5 * 1024 * 1024    // 5 MB max request size
)
public class ResumeUploadServlet extends HttpServlet {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private com.example.Online_job_portal.service.UserService userService;

    @Override
    public void init() throws ServletException {
        super.init();
        var ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (ctx != null) {
            if (applicationService == null) {
                applicationService = ctx.getBean(ApplicationService.class);
            }
            if (userService == null) {
                userService = ctx.getBean(com.example.Online_job_portal.service.UserService.class);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object userIdObj = session != null ? session.getAttribute("userId") : null;
        String userName = session != null ? (String) session.getAttribute("userName") : "JobSeeker";

        if (userIdObj == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=auth_required");
            return;
        }

        int jobSeekerId = (Integer) userIdObj;
        String jobIdStr = req.getParameter("jobId");
        if (jobIdStr == null || jobIdStr.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/jobseeker/jobs?error=" + encode("Invalid Job ID"));
            return;
        }

        int jobId = Integer.parseInt(jobIdStr);

        // Process file part
        Part filePart = null;
        try {
            filePart = req.getPart("resumeFile");
        } catch (Exception e) {
            resp.sendRedirect(req.getContextPath() + "/jobseeker/jobs?error=" + encode("File exceeds maximum allowed size of 2 MB"));
            return;
        }

        String coverLetter = req.getParameter("coverLetter");

        if (filePart == null || filePart.getSize() <= 0) {
            // Only allow using the seeker's pre-verified profile resume (no arbitrary user input)
            if (userService != null) {
                var seekerOpt = userService.findUserById(jobSeekerId);
                if (seekerOpt.isPresent() && ValidationUtil.isNotBlank(seekerOpt.get().getDefaultResumePath())) {
                    submitApplication(jobId, jobSeekerId, seekerOpt.get().getDefaultResumePath(), userName, coverLetter, req, resp);
                    return;
                }
            }

            resp.sendRedirect(req.getContextPath() + "/jobseeker/jobs?error=" + encode("Please select a verified resume file (PDF or DOCX)"));
            return;
        }

        // Validate size (2MB)
        if (!ValidationUtil.isValidResumeSize(filePart.getSize())) {
            resp.sendRedirect(req.getContextPath() + "/jobseeker/jobs?error=" + encode("File size exceeds 2 MB limit"));
            return;
        }

        // Validate extension (PDF or DOCX)
        String submittedFilename = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        if (!ValidationUtil.isValidResumeExtension(submittedFilename)) {
            resp.sendRedirect(req.getContextPath() + "/jobseeker/jobs?error=" + encode("Invalid file format. Only PDF and DOCX files are accepted."));
            return;
        }

        // Validate MIME type
        if (!ValidationUtil.isValidResumeMimeType(filePart.getContentType())) {
            resp.sendRedirect(req.getContextPath() + "/jobseeker/jobs?error=" + encode("Invalid file MIME type. Only PDF and DOCX files are allowed."));
            return;
        }

        // Validate file signature / magic bytes to prevent spoofed extensions
        byte[] header = new byte[8];
        try (java.io.InputStream is = filePart.getInputStream()) {
            int read = is.read(header);
            if (read < 4 || !ValidationUtil.isValidFileSignature(header, submittedFilename)) {
                resp.sendRedirect(req.getContextPath() + "/jobseeker/jobs?error=" + encode("Security check failed: File header does not match a valid PDF or DOCX format."));
                return;
            }
        }

        // Store file safely outside webapp public paths using configurable upload directory
        String uploadDir = ValidationUtil.getResumeUploadDirectory();
        File dir = new File(uploadDir);

        String safeFilename = UUID.randomUUID().toString().substring(0, 8) + "_" + submittedFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        File destFile = new File(dir, safeFilename);
        filePart.write(destFile.getAbsolutePath());

        submitApplication(jobId, jobSeekerId, safeFilename, userName, coverLetter, req, resp);
    }

    private void submitApplication(int jobId, int jobSeekerId, String resumePath, String userName, String coverLetter,
                                   HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (applicationService != null) {
                applicationService.applyForJob(jobId, jobSeekerId, resumePath, userName, coverLetter);
            }
            resp.sendRedirect(req.getContextPath() + "/jobseeker/applications?success=" + encode("Application submitted successfully with your resume!"));
        } catch (ApplicationException e) {
            resp.sendRedirect(req.getContextPath() + "/jobseeker/jobs?error=" + encode(e.getMessage()));
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
