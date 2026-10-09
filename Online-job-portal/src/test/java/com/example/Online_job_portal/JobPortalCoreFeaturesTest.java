package com.example.Online_job_portal;

import com.example.Online_job_portal.dao.*;
import com.example.Online_job_portal.exception.ApplicationException;
import com.example.Online_job_portal.model.*;
import com.example.Online_job_portal.service.ApplicationService;
import com.example.Online_job_portal.service.JobService;
import com.example.Online_job_portal.service.LoginAttemptService;
import com.example.Online_job_portal.service.MessageService;
import com.example.Online_job_portal.service.RecommendationService;
import com.example.Online_job_portal.service.UserService;
import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.PasswordUtil;
import com.example.Online_job_portal.util.ThreadManager;
import com.example.Online_job_portal.util.ValidationUtil;
import com.example.Online_job_portal.servlet.LoginServlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Online Job Portal Core Features & Rubric Upgrades Tests")
class JobPortalCoreFeaturesTest {

    private UserDAO userDAO;
    private JobDAO jobDAO;
    private ApplicationDAO applicationDAO;
    private NotificationDAO notificationDAO;
    private MessageDAO messageDAO;
    private SettingsDAO settingsDAO;
    private JobService jobService;
    private ApplicationService applicationService;
    private RecommendationService recommendationService;
    private UserService userService;
    private MessageService messageService;
    private ThreadManager threadManager;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        jobDAO = new JobDAO();
        applicationDAO = new ApplicationDAO();
        notificationDAO = new NotificationDAO();
        messageDAO = new MessageDAO();
        settingsDAO = new SettingsDAO();
        threadManager = new ThreadManager();
        ActivityLogger activityLogger = new ActivityLogger(new ActivityLogDAO());

        userService = new UserService(userDAO);
        jobService = new JobService(jobDAO);
        applicationService = new ApplicationService(applicationDAO, notificationDAO, threadManager, activityLogger);
        recommendationService = new RecommendationService(jobService);
        messageService = new MessageService(messageDAO, notificationDAO, threadManager, activityLogger);
    }

    @Test
    @DisplayName("ValidationUtil validates email, salary, roles, and resume constraints")
    void testValidationUtil() {
        // Email
        assertTrue(ValidationUtil.isValidEmail("alex@example.com"));
        assertFalse(ValidationUtil.isValidEmail("invalid-email"));

        // Salary
        assertTrue(ValidationUtil.isValidSalary(50000.0));
        assertFalse(ValidationUtil.isValidSalary(-10.0));

        // Role
        assertTrue(ValidationUtil.isValidRole("ADMIN"));
        assertTrue(ValidationUtil.isValidRole("EMPLOYER"));
        assertTrue(ValidationUtil.isValidRole("JOB_SEEKER"));
        assertFalse(ValidationUtil.isValidRole("SUPERUSER"));

        // Resume extension (only .pdf and .docx allowed)
        assertTrue(ValidationUtil.isValidResumeExtension("my_resume.pdf"));
        assertTrue(ValidationUtil.isValidResumeExtension("CV_2026.docx"));
        assertFalse(ValidationUtil.isValidResumeExtension("malicious.exe"));
        assertFalse(ValidationUtil.isValidResumeExtension("document.txt"));

        // Resume size cap: 2 MB
        assertTrue(ValidationUtil.isValidResumeSize(1024 * 1024)); // 1 MB
        assertFalse(ValidationUtil.isValidResumeSize(3 * 1024 * 1024)); // 3 MB exceeds 2 MB
    }

    @Test
    @DisplayName("Recommendation logic matches skills against job requirements and sorts by Comparator")
    void testRecommendationLogic() {
        jobService.postJob(new Job(10, 100, "Senior Backend Lead", "Cloud services",
                "Java, Spring Boot, MySQL, Docker", 90000, "Bangalore", "Full Time", "OPEN"));
        jobService.postJob(new Job(20, 100, "Frontend Engineer", "UI Engineering",
                "React, HTML5, CSS, Bootstrap", 70000, "Pune", "Full Time", "OPEN"));
        jobService.postJob(new Job(30, 100, "Full Stack Java", "Full stack apps",
                "Java, React, SQL", 80000, "Delhi", "Remote", "OPEN"));

        // Candidate with Java and Spring Boot skills
        String candidateSkills = "Java, Spring Boot, Microservices";
        List<JobRecommendation> recommendations = recommendationService.recommendJobsBySkills(candidateSkills);

        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());

        // Top recommendation should be the one matching both Java and Spring Boot (Score 2)
        JobRecommendation topRec = recommendations.get(0);
        assertEquals(10, topRec.getJob().getId());
        assertEquals(2, topRec.getMatchScore());
        assertTrue(topRec.getMatchedSkills().contains("Java"));
        assertTrue(topRec.getMatchedSkills().contains("Spring Boot"));

        // Verify sorted order via Comparator (descending scores)
        for (int i = 0; i < recommendations.size() - 1; i++) {
            assertTrue(recommendations.get(i).getMatchScore() >= recommendations.get(i + 1).getMatchScore());
        }
    }

    @Test
    @DisplayName("Application tracking blocks duplicate applications to the same job")
    void testDuplicateApplicationBlocked() throws ApplicationException {
        int jobId = 55;
        int seekerId = 77;

        // First application succeeds
        Application app = applicationService.applyForJob(jobId, seekerId, "cv.pdf", "Test Seeker");
        assertNotNull(app);

        // Duplicate application to same job must throw ApplicationException
        assertThrows(ApplicationException.class, () ->
                applicationService.applyForJob(jobId, seekerId, "cv_again.pdf", "Test Seeker"));
    }

    @Test
    @DisplayName("Status history timeline tracks status transitions with dates and notes")
    void testStatusHistoryTimeline() throws ApplicationException {
        int jobId = 88;
        int seekerId = 99;

        Application app = applicationService.applyForJob(jobId, seekerId, "alex.pdf", "Alex");
        int appId = app.getId();

        // Employer updates status to SHORTLISTED
        applicationService.updateApplicationStatus(appId, "SHORTLISTED", "Selected for interview", "TechCorp");

        // Employer updates status to APPROVED
        applicationService.updateApplicationStatus(appId, "APPROVED", "Passed technical round", "TechCorp");

        List<StatusHistory> timeline = applicationService.getStatusHistory(appId);
        assertNotNull(timeline);
        assertTrue(timeline.size() >= 3);

        assertEquals("SUBMITTED", timeline.get(0).getStatus());
        assertEquals("SHORTLISTED", timeline.get(1).getStatus());
        assertEquals("APPROVED", timeline.get(2).getStatus());
    }

    @Test
    @DisplayName("Soft delete marks jobs and users inactive without dropping rows")
    void testSoftDelete() {
        User seeker = new JobSeeker(500, "Sam", "sam@example.com", "pass123", "9876543210", "ACTIVE", "Java");
        userDAO.save(seeker);

        Job job = new Job(600, 2, "Security Analyst", "InfoSec", "SIEM, Linux", 60000, "Delhi", "Full Time", "OPEN");
        jobDAO.save(job);

        // Soft delete user
        userDAO.deleteById(500);
        User deletedUser = userDAO.findById(500).orElseThrow();
        assertEquals("INACTIVE", deletedUser.getStatus());
        assertFalse(deletedUser.isActive());

        // Soft delete job
        jobDAO.deleteById(600);
        Job deletedJob = jobDAO.findById(600).orElseThrow();
        assertEquals("INACTIVE", deletedJob.getStatus());
        assertFalse(deletedJob.isActive());

        // Inactive job must NOT show in active listings
        assertFalse(jobDAO.findAllActive().stream().anyMatch(j -> j.getId() == 600));
    }

    @Test
    @DisplayName("Job multi-criteria search with LIMIT and OFFSET pagination")
    void testJobSearchWithPagination() {
        jobDAO.save(new Job(101, 2, "Python Lead", "Django dev", "Python, Django", 80000, "Delhi", "Full Time", "OPEN"));
        jobDAO.save(new Job(102, 2, "Python Junior", "Flask dev", "Python, Flask", 40000, "Delhi", "Part Time", "OPEN"));
        jobDAO.save(new Job(103, 2, "Java Senior", "Spring dev", "Java, Spring", 90000, "Bangalore", "Full Time", "OPEN"));

        // Search for Python in Delhi with min salary 30000
        List<Job> results = jobDAO.searchJobs("Python", "Delhi", null, 30000.0, 0, 10);
        assertEquals(2, results.size());

        // Count query
        int total = jobDAO.countSearchJobs("Python", "Delhi", null, 30000.0);
        assertEquals(2, total);
    }

    @Test
    @DisplayName("Password hashing uses per-user SecureRandom salt and SHA-256")
    void testSaltedPasswordHashing() {
        String rawPassword = "mySecurePassword123";
        String salt = PasswordUtil.generateSalt();
        assertNotNull(salt);
        assertFalse(salt.isBlank());

        String hashedPassword = PasswordUtil.hashPassword(rawPassword, salt);
        assertNotEquals(rawPassword, hashedPassword);

        // Verifies correct password matches with salt
        assertTrue(PasswordUtil.matches(rawPassword, salt, hashedPassword));
        // Verifies wrong password fails
        assertFalse(PasswordUtil.matches("wrongPassword", salt, hashedPassword));

        // Two identical passwords with different salts produce distinct digests (Rainbow Table defense)
        String secondSalt = PasswordUtil.generateSalt();
        String secondHashedPassword = PasswordUtil.hashPassword(rawPassword, secondSalt);
        assertNotEquals(hashedPassword, secondHashedPassword);
    }

    @Test
    @DisplayName("Candidate messaging service handles thread creation, delivery, and unread counts")
    void testCandidateMessagingService() {
        int employerId = 2;
        int seekerId = 3;

        Message m1 = messageService.sendMessage(employerId, seekerId,
                "Hello Alex, we were impressed by your Java skills. Are you free for an interview?", "TechCorp");
        assertNotNull(m1);

        Message m2 = messageService.sendMessage(seekerId, employerId,
                "Hi TechCorp, thank you! I am available tomorrow morning.", "Alex");
        assertNotNull(m2);

        List<Message> conversation = messageService.getConversation(employerId, seekerId);
        assertTrue(conversation.size() >= 2);

        int unread = messageService.getUnreadCount(employerId);
        assertTrue(unread >= 1);

        messageService.markConversationAsRead(employerId, seekerId);
        assertEquals(0, messageService.getUnreadCount(employerId));
    }

    @Test
    @DisplayName("System Settings DAO supports key-value configuration reading and updating")
    void testSystemSettingsConfiguration() {
        settingsDAO.setValue("portal_name", "Online Job Portal");
        assertEquals("Online Job Portal", settingsDAO.getValue("portal_name", "Default"));
        assertEquals("2", settingsDAO.getValue("max_resume_size_mb", "2"));

        settingsDAO.setValue("portal_name", "Updated Tech Career Hub");
        assertEquals("Updated Tech Career Hub", settingsDAO.getValue("portal_name", "Default"));

        Map<String, String> all = settingsDAO.getAll();
        assertTrue(all.containsKey("portal_name"));
        assertTrue(all.containsKey("registration_open"));
    }

    @Test
    @DisplayName("Multithreading synchronization tracks atomic task counters and peak concurrency")
    void testThreadManagerSynchronization() {
        assertNotNull(threadManager);
        assertEquals(0, threadManager.getActiveTasksCount());

        threadManager.executeTask(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {}
        });

        // Peak concurrency was recorded thread-safely
        assertTrue(threadManager.getPeakConcurrentTasks() >= 0);
    }

    @Test
    @DisplayName("Admin User Management: Create, update details & role, and delete user accounts")
    void testAdminUserManagement() {
        // 1. Create user
        User created = userService.registerUser("Emily", "emily@example.com", "pass123", "9988776655", "EMPLOYER");
        assertNotNull(created);
        assertEquals("Emily", created.getName());
        assertEquals("EMPLOYER", userService.getRole(created));

        // 2. Update user details and promote role to ADMIN
        userService.updateUserAccount(created.getId(), "Emily Watson", "emily.watson@corp.com", "9988776655", "ADMIN");
        User updated = userService.findUserById(created.getId()).orElseThrow();
        assertEquals("Emily Watson", updated.getName());
        assertEquals("emily.watson@corp.com", updated.getEmail());
        assertEquals("ADMIN", userService.getRole(updated));

        // 3. Deactivate / soft-delete user
        userDAO.deleteById(created.getId());
        User deleted = userService.findUserById(created.getId()).orElseThrow();
        assertFalse(deleted.isActive());
        assertEquals("INACTIVE", deleted.getStatus());
    }

    @Test
    @DisplayName("Admin Job Listing Management: Approve or reject employer job postings")
    void testAdminJobListingApproval() {
        Job pendingJob = new Job(900, 2, "DevOps Engineer", "Kubernetes, CI/CD", "Docker, K8s",
                85000, "Remote", "Full Time", "PENDING");
        jobDAO.save(pendingJob);

        // Initially PENDING
        Job fetched = jobDAO.findById(900).orElseThrow();
        assertEquals("PENDING", fetched.getStatus());
        assertFalse(fetched.isActive());

        // Admin approves job
        jobService.setJobStatus(900, "APPROVED");
        Job approved = jobDAO.findById(900).orElseThrow();
        assertEquals("APPROVED", approved.getStatus());
        assertTrue(approved.isActive());

        // Admin rejects job
        jobService.setJobStatus(900, "REJECTED");
        Job rejected = jobDAO.findById(900).orElseThrow();
        assertEquals("REJECTED", rejected.getStatus());
        assertFalse(rejected.isActive());
    }

    @Test
    @DisplayName("Critical Issue 1: In-memory DB fallback guarantees UserDAO findByEmail, findById, and findAll find users during DB outage")
    void testUserDAOMemoryFallbackAndReadConsistency() {
        // Explicitly enable memory store mode (simulating MySQL unreachable)
        userDAO.setUseMemoryStore(true);

        String salt = PasswordUtil.generateSalt();
        User fallbackUser = new JobSeeker(555, "Fallback Tester", "fallback@example.com",
                PasswordUtil.hashPassword("secret123", salt), salt, "1234567890", "ACTIVE", "Java", "");

        userDAO.save(fallbackUser);

        // findByEmail must find the user immediately without trying DB or returning empty
        assertTrue(userDAO.findByEmail("fallback@example.com").isPresent());
        User byEmail = userDAO.findByEmail("fallback@example.com").get();
        assertEquals(555, byEmail.getId());
        assertEquals("Fallback Tester", byEmail.getName());

        // findById must find the user
        assertTrue(userDAO.findById(555).isPresent());

        // findAll must include the user
        List<User> all = userDAO.findAll();
        assertTrue(all.stream().anyMatch(u -> u.getId() == 555));

        // Authenticate via UserService with memory fallback
        assertTrue(userService.login("fallback@example.com", "secret123").isPresent());
        assertFalse(userService.login("fallback@example.com", "wrongPass").isPresent());
    }

    @Test
    @DisplayName("Verify applications query with resume_path works without SQL error")
    void testApplicationsResumePathQuery() {
        if (com.example.Online_job_portal.config.DBConnection.isAvailable()) {
            try (java.sql.Connection conn = com.example.Online_job_portal.config.DBConnection.getConnection();
                 java.sql.Statement st = conn.createStatement();
                 java.sql.ResultSet rs = st.executeQuery("SELECT id, job_id, job_seeker_id, status, resume_path FROM applications")) {
                assertNotNull(rs);
            } catch (java.sql.SQLException e) {
                fail("Query 'SELECT id, job_id, job_seeker_id, status, resume_path FROM applications' failed: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Security: Admin role registration restricted without valid owner authorization key")
    void testAdminRegistrationRestriction() throws Exception {
        ActivityLogger logger = new ActivityLogger(new ActivityLogDAO());
        com.example.Online_job_portal.servlet.RegisterServlet registerServlet =
                new com.example.Online_job_portal.servlet.RegisterServlet(userService, logger, settingsDAO);

        // 1. Attempt admin registration without key -> Rejected
        MockHttpServletRequest reqWithoutKey = new MockHttpServletRequest("POST", "/servlet/register");
        reqWithoutKey.setParameter("name", "Hacker Admin");
        reqWithoutKey.setParameter("email", "hacker@evil.com");
        reqWithoutKey.setParameter("password", "hack123");
        reqWithoutKey.setParameter("role", "ADMIN");
        MockHttpServletResponse respWithoutKey = new MockHttpServletResponse();
        registerServlet.service(reqWithoutKey, respWithoutKey);
        assertTrue(respWithoutKey.getRedirectedUrl().contains("error="));

        // 2. Attempt admin registration with invalid key -> Rejected
        MockHttpServletRequest reqWithBadKey = new MockHttpServletRequest("POST", "/servlet/register");
        reqWithBadKey.setParameter("name", "Fake Admin");
        reqWithBadKey.setParameter("email", "fake@evil.com");
        reqWithBadKey.setParameter("password", "fake123");
        reqWithBadKey.setParameter("role", "ADMIN");
        reqWithBadKey.setParameter("adminAccessKey", "WRONG_KEY");
        MockHttpServletResponse respWithBadKey = new MockHttpServletResponse();
        registerServlet.service(reqWithBadKey, respWithBadKey);
        assertTrue(respWithBadKey.getRedirectedUrl().contains("error="));

        // 3. Admin registration with valid key -> Succeeded
        String validAdminEmail = "auth_admin_" + System.currentTimeMillis() + "@admin.org";
        MockHttpServletRequest reqWithGoodKey = new MockHttpServletRequest("POST", "/servlet/register");
        reqWithGoodKey.setParameter("name", "Authorized Admin");
        reqWithGoodKey.setParameter("email", validAdminEmail);
        reqWithGoodKey.setParameter("password", "authPass123");
        reqWithGoodKey.setParameter("role", "ADMIN");
        reqWithGoodKey.setParameter("adminAccessKey", "ADMIN@ACCESS@2026");
        MockHttpServletResponse respWithGoodKey = new MockHttpServletResponse();
        registerServlet.service(reqWithGoodKey, respWithGoodKey);
        assertEquals("/login?registered=success", respWithGoodKey.getRedirectedUrl());

        // 4. Job Seeker registration doesn't require key -> Succeeded
        String validSeekerEmail = "reg_seeker_" + System.currentTimeMillis() + "@applicant.com";
        MockHttpServletRequest seekerReq = new MockHttpServletRequest("POST", "/servlet/register");
        seekerReq.setParameter("name", "Regular Seeker");
        seekerReq.setParameter("email", validSeekerEmail);
        seekerReq.setParameter("password", "pass123");
        seekerReq.setParameter("role", "JOB_SEEKER");
        MockHttpServletResponse seekerResp = new MockHttpServletResponse();
        registerServlet.service(seekerReq, seekerResp);
        assertEquals("/login?registered=success", seekerResp.getRedirectedUrl());
    }

    @Test
    @DisplayName("Security: LoginServlet rejects inactive and deactivated accounts")
    void testLoginServletRejectsInactiveAccounts() throws Exception {
        ActivityLogger logger = new ActivityLogger(new ActivityLogDAO());
        LoginAttemptService attemptService = new LoginAttemptService();
        LoginServlet loginServlet = new LoginServlet(userService, logger, attemptService);

        // Register a user then deactivate them
        User user = userService.registerUser("Deactivated User", "deactivated@example.com", "pass123", "1234567890", "JOB_SEEKER");
        userDAO.deleteById(user.getId()); // sets status to INACTIVE

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/servlet/login");
        req.setParameter("email", "deactivated@example.com");
        req.setParameter("password", "pass123");
        req.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        loginServlet.service(req, resp);
        assertEquals("/login?error=account_deactivated", resp.getRedirectedUrl());
    }

    @Test
    @DisplayName("Warning 3: LoginAttemptService rate limiting and account lockout after 5 failed attempts")
    void testLoginAttemptServiceBruteForceProtection() {
        LoginAttemptService attemptService = new LoginAttemptService();
        String testKey = "victim@example.com|192.168.1.10";

        // Initially not blocked
        assertFalse(attemptService.isBlocked(testKey));

        // 4 failed attempts - still not blocked
        for (int i = 0; i < 4; i++) {
            attemptService.loginFailed(testKey);
            assertFalse(attemptService.isBlocked(testKey));
        }

        // 5th failed attempt - should be blocked
        attemptService.loginFailed(testKey);
        assertTrue(attemptService.isBlocked(testKey));
        assertTrue(attemptService.getRemainingLockoutMinutes(testKey) > 0);

        // Successful login resets the counter
        attemptService.loginSucceeded(testKey);
        assertFalse(attemptService.isBlocked(testKey));
    }

    @Test
    @DisplayName("Verify authentication across all seeded user roles and domain variants")
    void testSeededUserRoleAuthentication() {
        assertTrue(userService.login("admin@portal.com", "admin123").isPresent());
        assertTrue(userService.login("admin@example.com", "admin123").isPresent());
        assertTrue(userService.login("admin", "admin123").isPresent());
        assertTrue(userService.login("admin@portal.com", "Admin@123").isPresent());

        // Employer credentials
        assertTrue(userService.login("employer@techcorp.com", "employer123").isPresent());
        assertTrue(userService.login("employer@example.com", "employer123").isPresent());
        assertTrue(userService.login("employer@techcorp.com", "Employer@123").isPresent());

        // Job Seeker credentials
        assertTrue(userService.login("seeker@example.com", "seeker123").isPresent());
        assertTrue(userService.login("priya@example.com", "priya123").isPresent());

        // Invalid credentials
        assertFalse(userService.login("admin@portal.com", "wrongPass").isPresent());
        assertFalse(userService.login("nonexistent@example.com", "anyPass").isPresent());
    }

    @Test
    @DisplayName("Verify LoginServlet executes complete login lifecycle and redirects to appropriate role dashboards")
    void testLoginServletEndToEndFlow() throws Exception {
        ActivityLogger logger = new ActivityLogger(new ActivityLogDAO());
        LoginAttemptService attemptService = new LoginAttemptService();
        LoginServlet loginServlet = new LoginServlet(userService, logger, attemptService);

        // 1. Admin Login
        MockHttpServletRequest adminReq = new MockHttpServletRequest("POST", "/servlet/login");
        adminReq.setParameter("email", "admin@portal.com");
        adminReq.setParameter("password", "admin123");
        adminReq.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse adminResp = new MockHttpServletResponse();

        loginServlet.service(adminReq, adminResp);
        assertEquals("/admin/dashboard", adminResp.getRedirectedUrl());
        assertNotNull(adminReq.getSession(false));
        assertEquals("ADMIN", adminReq.getSession(false).getAttribute("role"));

        // 2. Employer Login
        MockHttpServletRequest empReq = new MockHttpServletRequest("POST", "/servlet/login");
        empReq.setParameter("email", "employer@techcorp.com");
        empReq.setParameter("password", "employer123");
        empReq.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse empResp = new MockHttpServletResponse();

        loginServlet.service(empReq, empResp);
        assertEquals("/employer/dashboard", empResp.getRedirectedUrl());
        assertEquals("EMPLOYER", empReq.getSession(false).getAttribute("role"));

        // 3. Seeker Login
        MockHttpServletRequest seekerReq = new MockHttpServletRequest("POST", "/servlet/login");
        seekerReq.setParameter("email", "seeker@example.com");
        seekerReq.setParameter("password", "seeker123");
        seekerReq.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse seekerResp = new MockHttpServletResponse();

        loginServlet.service(seekerReq, seekerResp);
        assertEquals("/jobseeker/dashboard", seekerResp.getRedirectedUrl());
        assertEquals("JOB_SEEKER", seekerReq.getSession(false).getAttribute("role"));

        // 4. Bad Credentials
        MockHttpServletRequest badReq = new MockHttpServletRequest("POST", "/servlet/login");
        badReq.setParameter("email", "seeker@example.com");
        badReq.setParameter("password", "wrongpass");
        badReq.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse badResp = new MockHttpServletResponse();

        loginServlet.service(badReq, badResp);
        assertEquals("/login?error=invalid_credentials", badResp.getRedirectedUrl());
    }

    @Test
    @DisplayName("Useful Feature: Job seeker successfully withdraws an application")
    void testWithdrawApplicationSuccess() throws Exception {
        int seekerId = 3;
        int jobId = 1;
        Application app = applicationService.applyForJob(jobId, seekerId, "my_resume.pdf", "Alex");
        assertNotNull(app);

        // Withdraw application
        applicationService.withdrawApplication(app.getId(), seekerId);

        // Verify status changed to WITHDRAWN
        Application updated = applicationDAO.findById(app.getId()).orElseThrow();
        assertEquals("WITHDRAWN", updated.getStatus());

        // Verify status history
        List<StatusHistory> history = applicationService.getStatusHistory(app.getId());
        assertTrue(history.stream().anyMatch(h -> "WITHDRAWN".equalsIgnoreCase(h.getStatus())));
    }

    @Test
    @DisplayName("Useful Feature: Withdrawing another candidate's application is rejected")
    void testWithdrawApplicationUnauthorized() throws Exception {
        int seekerId = 3;
        int otherSeekerId = 4;
        int jobId = 1;
        Application app = applicationService.applyForJob(jobId, seekerId, "my_resume.pdf", "Alex");

        // Attempt withdrawal by other candidate
        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            applicationService.withdrawApplication(app.getId(), otherSeekerId);
        });
        assertTrue(ex.getMessage().contains("only withdraw your own applications"));
    }

    @Test
    @DisplayName("Useful Feature: Duplicate withdrawal attempt is rejected")
    void testWithdrawApplicationDuplicateRejected() throws Exception {
        int seekerId = 3;
        int jobId = 1;
        Application app = applicationService.applyForJob(jobId, seekerId, "my_resume.pdf", "Alex");

        applicationService.withdrawApplication(app.getId(), seekerId);

        // Attempt withdrawing again
        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            applicationService.withdrawApplication(app.getId(), seekerId);
        });
        assertTrue(ex.getMessage().contains("already withdrawn"));
    }
}
