package com.example.Online_job_portal.config;

import com.example.Online_job_portal.dao.ApplicationDAO;
import com.example.Online_job_portal.dao.JobDAO;
import com.example.Online_job_portal.dao.UserDAO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes realistic demo dataset on application startup for examiners and demo showcases.
 */
@Component
public class DataInitializer implements CommandLineRunner {

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
        userDAO.seedInitialUsers();
        jobDAO.seedInitialJobs();
        applicationDAO.seedInitialApplications();

        if (DBConnection.isAvailable()) {
            syncMySQLDemoDataset();
        }
    }

    private void syncMySQLDemoDataset() {
        try (java.sql.Connection conn = DBConnection.getConnection();
             java.sql.Statement st = conn.createStatement()) {

            // Automatically upgrade users table if columns are missing from older setups
            try { st.executeUpdate("ALTER TABLE users ADD COLUMN salt VARCHAR(64) DEFAULT ''"); } catch (java.sql.SQLException ignored) {}
            try { st.executeUpdate("ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE'"); } catch (java.sql.SQLException ignored) {}
            try { st.executeUpdate("ALTER TABLE users ADD COLUMN skills VARCHAR(255) DEFAULT ''"); } catch (java.sql.SQLException ignored) {}
            try { st.executeUpdate("ALTER TABLE users ADD COLUMN default_resume_path VARCHAR(255) DEFAULT ''"); } catch (java.sql.SQLException ignored) {}

            seedUserIfMissing(conn, "Admin User", "admin@portal.com", "admin123", "ADMIN");
            seedUserIfMissing(conn, "Admin Example", "admin@example.com", "admin123", "ADMIN");
            seedUserIfMissing(conn, "TechCorp Solutions", "employer@techcorp.com", "employer123", "EMPLOYER");
            seedUserIfMissing(conn, "Global Tech", "employer@example.com", "employer123", "EMPLOYER");
            seedUserIfMissing(conn, "Alex Kumar", "seeker@example.com", "seeker123", "JOB_SEEKER");
            seedUserIfMissing(conn, "Priya Sharma", "priya@example.com", "priya123", "JOB_SEEKER");
        } catch (Exception ignored) {
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
        } catch (java.sql.SQLException ignored) {
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (name, email, password, phone, role) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, hashed);
                ps.setString(4, "9876543210");
                ps.setString(5, role);
                ps.executeUpdate();
            } catch (java.sql.SQLException ignored2) {
            }
        }
    }
}
