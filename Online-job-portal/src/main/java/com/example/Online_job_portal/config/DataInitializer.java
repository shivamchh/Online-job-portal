package com.example.Online_job_portal.config;

import com.example.Online_job_portal.dao.ApplicationDAO;
import com.example.Online_job_portal.dao.JobDAO;
import com.example.Online_job_portal.dao.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes realistic demo dataset on application startup for examiners and demo showcases.
 * Supports configurable credentials via environment variables and logs all schema operations.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserDAO userDAO;
    private final JobDAO jobDAO;
    private final ApplicationDAO applicationDAO;

    public DataInitializer(UserDAO userDAO, JobDAO jobDAO, ApplicationDAO applicationDAO) {
        this.userDAO = userDAO;
        this.jobDAO = jobDAO;
        this.applicationDAO = applicationDAO;
    }

    @Override
    public void run(String... args) {
        String seedDemo = System.getenv("SEED_DEMO_DATA");
        if (seedDemo == null) {
            seedDemo = System.getProperty("SEED_DEMO_DATA", "true");
        }

        if (!"false".equalsIgnoreCase(seedDemo)) {
            userDAO.seedInitialUsers();
            jobDAO.seedInitialJobs();
            applicationDAO.seedInitialApplications();
        } else {
            log.info("SEED_DEMO_DATA is disabled; skipping in-memory demo data seeding.");
        }

        if (DBConnection.isAvailable()) {
            syncMySQLDemoDataset("false".equalsIgnoreCase(seedDemo));
        }
    }

    private void syncMySQLDemoDataset(boolean skipDemoUsers) {
        try (java.sql.Connection conn = DBConnection.getConnection();
             java.sql.Statement st = conn.createStatement()) {

            // Automatically upgrade users table if columns are missing from older setups
            safeExecuteUpdate(st, "ALTER TABLE users ADD COLUMN salt VARCHAR(64) DEFAULT ''", "users.salt");
            safeExecuteUpdate(st, "ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE'", "users.status");
            safeExecuteUpdate(st, "ALTER TABLE users ADD COLUMN skills VARCHAR(255) DEFAULT ''", "users.skills");
            safeExecuteUpdate(st, "ALTER TABLE users ADD COLUMN default_resume_path VARCHAR(255) DEFAULT ''", "users.default_resume_path");

            // Automatically upgrade applications table so SELECT id, job_id, job_seeker_id, status, resume_path FROM applications works
            safeExecuteUpdate(st, "ALTER TABLE applications ADD COLUMN resume_path VARCHAR(255)", "applications.resume_path");
            safeExecuteUpdate(st, "UPDATE applications SET resume_path = resume WHERE (resume_path IS NULL OR resume_path = '') AND resume IS NOT NULL", "sync resume_path");
            safeExecuteUpdate(st, "ALTER TABLE applications ADD COLUMN applied_date DATE", "applications.applied_date");
            safeExecuteUpdate(st, "UPDATE applications SET applied_date = DATE(applied_at) WHERE applied_date IS NULL AND applied_at IS NOT NULL", "sync applied_date");
            safeExecuteUpdate(st, "ALTER TABLE applications ADD COLUMN status_updated_at DATE", "applications.status_updated_at");
            safeExecuteUpdate(st, "UPDATE applications SET status_updated_at = applied_date WHERE status_updated_at IS NULL", "sync status_updated_at");
            safeExecuteUpdate(st, "ALTER TABLE applications ADD COLUMN notes VARCHAR(255) DEFAULT ''", "applications.notes");
            safeExecuteUpdate(st, "ALTER TABLE applications ADD COLUMN resume VARCHAR(255)", "applications.resume");
            safeExecuteUpdate(st, "UPDATE applications SET resume = resume_path WHERE (resume IS NULL OR resume = '') AND resume_path IS NOT NULL", "sync resume");

            // Automatically ensure settings table and defaults exist
            safeExecuteUpdate(st, "CREATE TABLE IF NOT EXISTS settings (" +
                    "key_name VARCHAR(100) PRIMARY KEY, " +
                    "key_value VARCHAR(255) NOT NULL, " +
                    "description VARCHAR(255), " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)", "create settings table");
            safeExecuteUpdate(st, "INSERT IGNORE INTO settings (key_name, key_value, description) VALUES " +
                    "('portal_name', 'Online Job Portal', 'Display title of the portal'), " +
                    "('max_resume_size_mb', '2', 'Maximum allowable resume size in Megabytes'), " +
                    "('registration_open', 'true', 'Whether new candidate registration is enabled (true/false)'), " +
                    "('admin_access_key', 'ADMIN@ACCESS@2026', 'Owner authorization key required to register an Admin account'), " +
                    "('require_account_approval', 'false', 'Whether new accounts require owner approval before login (true/false)'), " +
                    "('maintenance_mode', 'false', 'Portal maintenance lock'), " +
                    "('contact_email', 'support@jobportal.com', 'System administrator contact email')", "seed settings defaults");

            // Automatically ensure notifications table exists in MySQL
            safeExecuteUpdate(st, "CREATE TABLE IF NOT EXISTS notifications (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "title VARCHAR(150) NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "is_read BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)", "create notifications table");

            // Seed initial notification for demo job seeker if none exist
            safeExecuteUpdate(st, "INSERT INTO notifications (user_id, title, message, is_read) " +
                    "SELECT 105, '🎉 Welcome to JobSphere!', 'Your job seeker account is active. Browse open job listings and track your applications!', FALSE " +
                    "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE user_id = 105)", "seed demo notification");

            // Automatically migrate any legacy unhashed/plaintext passwords to PBKDF2 hashes
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, email, password FROM users WHERE LENGTH(password) < 60 OR password = 'admin123'");
                 java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int uid = rs.getInt("id");
                    String email = rs.getString("email");
                    String raw = rs.getString("password");
                    if ("pbkdf2:65536:c2FsdDFfYWRtaW4=:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".equals(raw)) {
                        raw = "admin123";
                    }
                    String salt = com.example.Online_job_portal.util.PasswordUtil.generateSalt();
                    String hashed = com.example.Online_job_portal.util.PasswordUtil.hashPassword(raw, salt);
                    try (java.sql.PreparedStatement updatePs = conn.prepareStatement(
                            "UPDATE users SET password = ?, salt = ? WHERE id = ?")) {
                        updatePs.setString(1, hashed);
                        updatePs.setString(2, salt);
                        updatePs.setInt(3, uid);
                        updatePs.executeUpdate();
                        log.info("Migrated user [{}] to slow PBKDF2 salted hash", email);
                    }
                }
            } catch (Exception e) {
                log.debug("Password migration notice: {}", e.getMessage());
            }

            if (!skipDemoUsers) {
                String envAdminEmail = getEnvOrProp("ADMIN_EMAIL", "admin@portal.com");
                String envAdminPassword = getEnvOrProp("ADMIN_PASSWORD", "admin123");
                if ("admin123".equals(envAdminPassword)) {
                    log.warn("Seeding initial administrator account with default credentials. Set ADMIN_EMAIL and ADMIN_PASSWORD environment variables for production security.");
                }

                seedUserIfMissing(conn, "Admin User", envAdminEmail, envAdminPassword, "ADMIN");
                seedUserIfMissing(conn, "Admin Example", "admin@example.com", "admin123", "ADMIN");
                seedUserIfMissing(conn, "TechCorp Solutions", "employer@techcorp.com", "employer123", "EMPLOYER");
                seedUserIfMissing(conn, "Global Tech", "employer@example.com", "employer123", "EMPLOYER");
                seedUserIfMissing(conn, "Alex Kumar", "seeker@example.com", "seeker123", "JOB_SEEKER");
                seedUserIfMissing(conn, "Priya Sharma", "priya@example.com", "priya123", "JOB_SEEKER");

                seedJobsIfEmpty(conn);
                seedApplicationsIfEmpty(conn);
            }
        } catch (Exception e) {
            log.error("Failed to synchronize MySQL dataset/schema: {}", e.getMessage(), e);
        }
    }

    private void safeExecuteUpdate(java.sql.Statement st, String sql, String operationDesc) {
        try {
            st.executeUpdate(sql);
        } catch (java.sql.SQLException e) {
            log.debug("Schema migration notice for [{}]: {}", operationDesc, e.getMessage());
        }
    }

    private void seedUserIfMissing(java.sql.Connection conn, String name, String email, String password, String role) {
        String checkSql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, email);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        } catch (java.sql.SQLException e) {
            log.warn("Could not check existence of user {}: {}", email, e.getMessage());
            return;
        }

        String salt = com.example.Online_job_portal.util.PasswordUtil.generateSalt();
        String hashed = com.example.Online_job_portal.util.PasswordUtil.hashPassword(password, salt);
        String insertSql = "INSERT INTO users (name, email, password, salt, phone, role, status, skills, default_resume_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hashed);
            ps.setString(4, salt);
            ps.setString(5, "9876543210");
            ps.setString(6, role);
            ps.setString(7, "ACTIVE");
            ps.setString(8, "JOB_SEEKER".equals(role) ? "Java, Spring Boot, MySQL, REST API" : "");
            ps.setString(9, "JOB_SEEKER".equals(role) ? "alex_resume.pdf" : "");
            ps.executeUpdate();
            log.info("Seeded initial account for [{}] with role [{}]", email, role);
        } catch (java.sql.SQLException e) {
            log.debug("Falling back to basic user schema insert for {}: {}", email, e.getMessage());
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (name, email, password, phone, role) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, hashed);
                ps.setString(4, "9876543210");
                ps.setString(5, role);
                ps.executeUpdate();
            } catch (java.sql.SQLException e2) {
                log.error("Failed to seed user {}: {}", email, e2.getMessage());
            }
        }
    }

    private static String getEnvOrProp(String name, String defaultValue) {
        String val = System.getenv(name);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        val = System.getProperty(name);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        return defaultValue;
    }

    private void seedJobsIfEmpty(java.sql.Connection conn) {
        String checkSql = "SELECT COUNT(*) FROM jobs";
        try (java.sql.Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // already has jobs
            }
        } catch (java.sql.SQLException e) {
            log.debug("jobs table check notice: {}", e.getMessage());
            return;
        }

        int employerId = 1;
        try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE role = 'EMPLOYER' LIMIT 1");
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                employerId = rs.getInt(1);
            }
        } catch (Exception ignored) {}

        String insertSql = "INSERT INTO jobs (employer_id, title, description, requirements, salary, location, job_type, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(insertSql)) {
            java.time.LocalDate now = java.time.LocalDate.now();
            Object[][] initialJobs = new Object[][]{
                    {employerId, "Senior Java Developer", "Develop scalable cloud services, enterprise applications, and secure REST APIs.", "Java 17, Spring Boot, MySQL, REST API", 85000.0, "Bangalore", "Full Time", "OPEN", java.sql.Date.valueOf(now.minusDays(12))},
                    {employerId, "Frontend React Engineer", "Build responsive modern single-page applications.", "JavaScript, React, Bootstrap, HTML5, CSS3", 65000.0, "Pune", "Full Time", "OPEN", java.sql.Date.valueOf(now.minusDays(8))},
                    {employerId, "Full Stack Java Specialist", "Lead end-to-end web engineering projects.", "Java, Spring Boot, React, MySQL", 75000.0, "Delhi", "Remote", "OPEN", java.sql.Date.valueOf(now.minusDays(5))},
                    {employerId, "QA Automation Engineer", "Design automated test suites using Selenium and JUnit.", "Java, Selenium, JUnit, TestNG, SQL", 55000.0, "Hyderabad", "Full Time", "OPEN", java.sql.Date.valueOf(now.minusDays(2))},
                    {employerId, "Cloud Infrastructure Engineer", "Manage AWS cloud deployments and CI/CD pipelines.", "AWS, Docker, Kubernetes, Linux", 90000.0, "Remote", "Full Time", "OPEN", java.sql.Date.valueOf(now)}
            };
            for (Object[] j : initialJobs) {
                ps.setInt(1, (Integer) j[0]);
                ps.setString(2, (String) j[1]);
                ps.setString(3, (String) j[2]);
                ps.setString(4, (String) j[3]);
                ps.setDouble(5, (Double) j[4]);
                ps.setString(6, (String) j[5]);
                ps.setString(7, (String) j[6]);
                ps.setString(8, (String) j[7]);
                ps.setDate(9, (java.sql.Date) j[8]);
                ps.addBatch();
            }
            ps.executeBatch();
            log.info("Seeded initial jobs into MySQL database.");
        } catch (java.sql.SQLException e) {
            log.warn("Could not seed initial jobs into MySQL: {}", e.getMessage());
        }
    }

    private void seedApplicationsIfEmpty(java.sql.Connection conn) {
        String checkSql = "SELECT COUNT(*) FROM applications";
        try (java.sql.Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // already has applications
            }
        } catch (java.sql.SQLException e) {
            log.debug("applications table check notice: {}", e.getMessage());
            return;
        }

        int seekerId = 1;
        try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE role = 'JOB_SEEKER' LIMIT 1");
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                seekerId = rs.getInt(1);
            }
        } catch (Exception ignored) {}

        int jobId = 1;
        try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM jobs LIMIT 1");
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                jobId = rs.getInt(1);
            }
        } catch (Exception ignored) {}

        String insertSql = "INSERT INTO applications (job_id, job_seeker_id, resume_path, status, applied_date, status_updated_at, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(insertSql)) {
            java.time.LocalDate now = java.time.LocalDate.now();
            Object[][] initialApps = new Object[][]{
                    {jobId, seekerId, "alex_resume.pdf", "SHORTLISTED", java.sql.Date.valueOf(now.minusDays(5)), java.sql.Date.valueOf(now.minusDays(2)), "Candidate shortlisted for technical round"},
                    {jobId, seekerId, "alex_resume.pdf", "APPROVED", java.sql.Date.valueOf(now.minusDays(3)), java.sql.Date.valueOf(now.minusDays(1)), "Candidate selected"},
                    {jobId, seekerId, "alex_resume.pdf", "SUBMITTED", java.sql.Date.valueOf(now), java.sql.Date.valueOf(now), "Under initial review"}
            };
            for (Object[] app : initialApps) {
                ps.setInt(1, (Integer) app[0]);
                ps.setInt(2, (Integer) app[1]);
                ps.setString(3, (String) app[2]);
                ps.setString(4, (String) app[3]);
                ps.setDate(5, (java.sql.Date) app[4]);
                ps.setDate(6, (java.sql.Date) app[5]);
                ps.setString(7, (String) app[6]);
                ps.addBatch();
            }
            ps.executeBatch();
            log.info("Seeded initial applications into MySQL database.");
        } catch (java.sql.SQLException e) {
            log.warn("Could not seed initial applications into MySQL: {}", e.getMessage());
        }
    }
}
