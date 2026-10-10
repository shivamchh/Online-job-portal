package com.example.Online_job_portal;

import com.example.Online_job_portal.dao.*;
import com.example.Online_job_portal.filter.AuthFilter;
import com.example.Online_job_portal.filter.CsrfFilter;
import com.example.Online_job_portal.filter.SecurityHeadersFilter;
import com.example.Online_job_portal.model.*;
import com.example.Online_job_portal.service.*;
import com.example.Online_job_portal.servlet.LogoutServlet;
import com.example.Online_job_portal.servlet.ResumeDownloadServlet;
import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.PasswordUtil;
import com.example.Online_job_portal.util.ValidationUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Security & Request Audit Verification Suite")
public class SecurityAndRequestAuditTest {

    private AuthFilter authFilter;
    private CsrfFilter csrfFilter;
    private SecurityHeadersFilter securityHeadersFilter;
    private ApplicationService applicationService;
    private JobService jobService;
    private UserService userService;
    private UserDAO userDAO;
    private JobDAO jobDAO;
    private ApplicationDAO applicationDAO;

    @BeforeEach
    void setUp() {
        authFilter = new AuthFilter();
        csrfFilter = new CsrfFilter();
        securityHeadersFilter = new SecurityHeadersFilter();

        userDAO = new UserDAO();
        jobDAO = new JobDAO();
        applicationDAO = new ApplicationDAO();

        userService = new UserService(userDAO);
        jobService = new JobService(jobDAO);
        ActivityLogger activityLogger = new ActivityLogger(new ActivityLogDAO());
        NotificationDAO notificationDAO = new NotificationDAO();
        com.example.Online_job_portal.util.ThreadManager threadManager = new com.example.Online_job_portal.util.ThreadManager();

        applicationService = new ApplicationService(applicationDAO, notificationDAO, threadManager, activityLogger);
    }

    @Test
    @DisplayName("High: Unauthenticated protected request returns 302 redirect or 401, never 200 OK")
    void testUnauthenticatedProtectedRequestReturnsRedirectOrUnauthorized() throws Exception {
        // Browser request: expects 302 redirect
        MockHttpServletRequest browserReq = new MockHttpServletRequest("GET", "/employer/applications");
        MockHttpServletResponse browserResp = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        authFilter.doFilter(browserReq, browserResp, filterChain);

        assertEquals(302, browserResp.getStatus(), "Browser request must yield 302 redirect, not 200 OK");
        assertTrue(browserResp.getRedirectedUrl().contains("/login?error=auth_required"));

        // API/JSON request: expects 401 Unauthorized
        MockHttpServletRequest apiReq = new MockHttpServletRequest("GET", "/employer/applications");
        apiReq.addHeader("Accept", "application/json");
        MockHttpServletResponse apiResp = new MockHttpServletResponse();

        authFilter.doFilter(apiReq, apiResp, filterChain);

        assertEquals(401, apiResp.getStatus(), "API request without credentials must yield 401 Unauthorized");
        assertTrue(apiResp.getContentType().contains("application/json"));
    }

    @Test
    @DisplayName("High: Role-based access control blocks unauthorized roles with redirect / 403")
    void testRoleBasedAccessControlBlocksUnauthorizedRole() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/employer/applications");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 105);
        session.setAttribute("userName", "Alex Seeker");
        session.setAttribute("role", "JOB_SEEKER");
        req.setSession(session);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        authFilter.doFilter(req, resp, new MockFilterChain());

        assertEquals(302, resp.getStatus());
        assertTrue(resp.getRedirectedUrl().contains("/access-denied"));
    }

    @Test
    @DisplayName("Critical: Employer cannot update another employer's application (Ownership check)")
    void testEmployerCannotUpdateAnotherEmployersApplication() {
        int employer1Id = 501;
        int employer2Id = 502;

        // Employer 1 posts a job
        Job job1 = new Job(901, employer1Id, "Senior Architect", "Desc", "Java", 100000.0, "NY", "Full Time", "OPEN", LocalDate.now());
        jobDAO.save(job1);

        // Seeker applies to Job 1
        Application app = new Application(801, job1.getId(), 301, "candidate_cv.pdf", "SUBMITTED", LocalDate.now(), LocalDate.now(), "Notes");
        applicationDAO.save(app);

        // Employer 2 attempts to change status of Employer 1's application -> must throw SecurityException
        assertThrows(SecurityException.class, () -> {
            applicationService.updateApplicationStatusByEmployer(app.getId(), employer2Id, "SHORTLISTED", "Unauthorized review", "Employer Two", jobService);
        }, "Employer 2 must not be able to modify Employer 1's application");

        // Employer 1 legitimately updates their own application -> succeeds
        assertDoesNotThrow(() -> {
            applicationService.updateApplicationStatusByEmployer(app.getId(), employer1Id, "SHORTLISTED", "Legitimate review", "Employer One", jobService);
        });
    }

    @Test
    @DisplayName("High: Unvalidated application status is rejected against whitelist enum")
    void testUnvalidatedApplicationStatusRejected() {
        assertThrows(IllegalArgumentException.class, () -> {
            applicationService.updateApplicationStatus(1, "HACKED_STATUS", "Invalid", "Reviewer");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            applicationService.updateApplicationStatus(1, "DROP TABLE applications;", "Malicious", "Reviewer");
        });

        // Valid status values pass
        for (String validStatus : ApplicationService.VALID_STATUSES) {
            assertDoesNotThrow(() -> {
                applicationService.updateApplicationStatus(1, validStatus, "Valid status check", "Reviewer");
            });
        }
    }

    @Test
    @DisplayName("Critical: Slow password hashing (PBKDF2) rejects plaintext fallback")
    void testPasswordSecurityAndNoPlaintextFallback() {
        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword("SuperSecret123", salt);

        // 1. Must use slow PBKDF2 hash prefix
        assertTrue(hashedPassword.startsWith("pbkdf2:65536:"));

        // 2. Correct password matches
        assertTrue(PasswordUtil.matches("SuperSecret123", salt, hashedPassword));

        // 3. Plaintext matching must be strictly rejected
        assertFalse(PasswordUtil.matches("SuperSecret123", "SuperSecret123"), "Plaintext match must not be accepted");
        assertFalse(PasswordUtil.matches("admin123", "admin123"), "Plaintext passwords must be rejected");

        // 4. Wrong password fails
        assertFalse(PasswordUtil.matches("WrongPassword", salt, hashedPassword));
    }

    @Test
    @DisplayName("Medium: Upload file validation checks MIME type and file signature magic bytes")
    void testFileSignatureAndMimeTypeValidation() {
        // Valid PDF magic bytes (%PDF)
        byte[] validPdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x35};
        assertTrue(ValidationUtil.isValidFileSignature(validPdfHeader, "resume.pdf"));
        assertTrue(ValidationUtil.isValidResumeMimeType("application/pdf"));

        // Valid DOCX magic bytes (PK\x03\x04)
        byte[] validDocxHeader = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00};
        assertTrue(ValidationUtil.isValidFileSignature(validDocxHeader, "resume.docx"));
        assertTrue(ValidationUtil.isValidResumeMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

        // Malicious file disguised with .pdf extension (e.g. executable script)
        byte[] fakePdfHeader = new byte[]{'<', '?', 'p', 'h', 'p', ' ', 'e', 'v'};
        assertFalse(ValidationUtil.isValidFileSignature(fakePdfHeader, "malicious.pdf"), "Spoofed PDF header must be rejected");

        // Invalid MIME type
        assertFalse(ValidationUtil.isValidResumeMimeType("application/x-msdownload"));
        assertFalse(ValidationUtil.isValidResumeMimeType("text/html"));
    }

    @Test
    @DisplayName("High: CSRF protection filter rejects missing or invalid tokens on state-changing requests")
    void testCsrfFilterProtectsStateChangingRequests() throws Exception {
        // 1. Missing CSRF token on servlet endpoint -> 403 Forbidden
        MockHttpServletRequest postServletReq = new MockHttpServletRequest("POST", "/servlet/jobs");
        MockHttpSession servletSession = new MockHttpSession();
        postServletReq.setSession(servletSession);

        MockHttpServletResponse servletResp = new MockHttpServletResponse();
        MockFilterChain servletChain = new MockFilterChain();
        csrfFilter.doFilter(postServletReq, servletResp, servletChain);
        assertEquals(403, servletResp.getStatus(), "POST to /servlet/jobs without CSRF token must be 403 Forbidden");

        // 2. Missing CSRF token on application servlet -> 403 Forbidden
        MockHttpServletRequest appPostReq = new MockHttpServletRequest("POST", "/servlet/applications");
        appPostReq.setSession(new MockHttpSession());
        MockHttpServletResponse appResp = new MockHttpServletResponse();
        csrfFilter.doFilter(appPostReq, appResp, new MockFilterChain());
        assertEquals(403, appResp.getStatus(), "POST to /servlet/applications without CSRF token must be 403 Forbidden");

        // 3. Invalid CSRF token -> 403 Forbidden
        postServletReq.setParameter("_csrf", "invalid_forged_token");
        MockHttpServletResponse respInvalid = new MockHttpServletResponse();
        csrfFilter.doFilter(postServletReq, respInvalid, new MockFilterChain());
        assertEquals(403, respInvalid.getStatus(), "POST with forged CSRF token must be 403 Forbidden");

        // 4. Valid CSRF token -> Passes filter chain
        String validToken = (String) servletSession.getAttribute(CsrfFilter.CSRF_TOKEN_SESSION_ATTR);
        postServletReq.setParameter("_csrf", validToken);
        MockHttpServletResponse respValid = new MockHttpServletResponse();
        csrfFilter.doFilter(postServletReq, respValid, new MockFilterChain());
        assertNotEquals(403, respValid.getStatus(), "POST with valid CSRF token must pass");
    }

    @Test
    @DisplayName("Medium: Security headers attached to responses (CSP, X-Content-Type-Options, Frame-Options)")
    void testSecurityHeadersAttachedToResponses() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        securityHeadersFilter.doFilter(req, resp, chain);

        assertEquals("nosniff", resp.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", resp.getHeader("X-Frame-Options"));
        assertEquals("strict-origin-when-cross-origin", resp.getHeader("Referrer-Policy"));
        assertNotNull(resp.getHeader("Content-Security-Policy"));
        assertTrue(resp.getHeader("Content-Security-Policy").contains("frame-ancestors 'none'"));
    }

    @Test
    @DisplayName("Medium: Logout uses POST request and properly invalidates session")
    void testLogoutUsesPostAndInvalidatesSession() throws Exception {
        LogoutServlet logoutServlet = new LogoutServlet();
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/servlet/logout");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 101);
        session.setAttribute("userName", "Admin");
        req.setSession(session);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        logoutServlet.service(req, resp);

        assertTrue(session.isInvalid(), "Session must be invalidated on logout");
        assertTrue(resp.getRedirectedUrl().contains("/login?logout=success"));
    }

    @Test
    @DisplayName("Deployment Readiness: /actuator/health is publicly accessible for cloud health checks")
    void testActuatorHealthIsPubliclyAccessible() throws Exception {
        MockHttpServletRequest healthReq = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse healthResp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        authFilter.doFilter(healthReq, healthResp, chain);
        // Must not be redirected to login
        assertNotEquals(302, healthResp.getStatus(), "Actuator health endpoint must not redirect to login");
        assertNotEquals(401, healthResp.getStatus(), "Actuator health endpoint must be public for container orchestrators");
    }

    @Test
    @DisplayName("Deployment Readiness: CSP allows Bootstrap, DataTables, and jQuery CDNs")
    void testCspAllowsRequiredCdns() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        securityHeadersFilter.doFilter(req, resp, new MockFilterChain());

        String csp = resp.getHeader("Content-Security-Policy");
        assertNotNull(csp);
        assertTrue(csp.contains("https://cdn.jsdelivr.net"), "CSP must permit jsdelivr (Bootstrap/Chart.js)");
        assertTrue(csp.contains("https://code.jquery.com"), "CSP must permit code.jquery.com (jQuery)");
        assertTrue(csp.contains("https://cdn.datatables.net"), "CSP must permit cdn.datatables.net (DataTables)");
    }
}
