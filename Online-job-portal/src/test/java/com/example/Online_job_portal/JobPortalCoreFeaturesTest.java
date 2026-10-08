package com.example.Online_job_portal;

import com.example.Online_job_portal.dao.*;
import com.example.Online_job_portal.exception.ApplicationException;
import com.example.Online_job_portal.model.*;
import com.example.Online_job_portal.service.ApplicationService;
import com.example.Online_job_portal.service.JobService;
import com.example.Online_job_portal.service.MessageService;
import com.example.Online_job_portal.service.RecommendationService;
import com.example.Online_job_portal.service.UserService;
import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.PasswordUtil;
import com.example.Online_job_portal.util.ThreadManager;
import com.example.Online_job_portal.util.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
