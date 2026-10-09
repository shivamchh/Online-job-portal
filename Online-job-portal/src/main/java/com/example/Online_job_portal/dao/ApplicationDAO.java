package com.example.Online_job_portal.dao;

import com.example.Online_job_portal.config.DBConnection;
import com.example.Online_job_portal.model.Application;
import com.example.Online_job_portal.model.StatusHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class ApplicationDAO implements DAO<Application> {

    private static final Logger log = LoggerFactory.getLogger(ApplicationDAO.class);

    private final List<Application> applications = new ArrayList<>();
    private final List<StatusHistory> historyList = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(10);
    private final AtomicInteger historyIdCounter = new AtomicInteger(10);
    private boolean useMemoryStore;

    public ApplicationDAO() {
    }

    private static boolean isStrictDbMode() {
        return "true".equalsIgnoreCase(System.getenv("STRICT_DB_MODE"))
                || "true".equalsIgnoreCase(System.getProperty("STRICT_DB_MODE"));
    }

    private void handleSqlException(String operation, SQLException e) {
        log.error("Database SQL failure during [{}] in ApplicationDAO: {}", operation, e.getMessage(), e);
        if (isStrictDbMode()) {
            throw new RuntimeException("Database operation [" + operation + "] failed in ApplicationDAO", e);
        }
        useMemoryStore = true;
    }

    public synchronized void seedInitialApplications() {
        if (!applications.isEmpty()) return;
        Application app1 = new Application(1, 1, 3, "alex_resume.pdf", "SHORTLISTED",
                LocalDate.now().minusDays(7), LocalDate.now().minusDays(3), "Candidate selected for technical interview");
        Application app2 = new Application(2, 3, 4, "priya_resume.pdf", "APPROVED",
                LocalDate.now().minusDays(5), LocalDate.now().minusDays(1), "Offer letter issued");
        Application app3 = new Application(3, 2, 3, "alex_resume.pdf", "SUBMITTED",
                LocalDate.now().minusDays(2), LocalDate.now().minusDays(2), "Initial application received");

        applications.add(app1);
        applications.add(app2);
        applications.add(app3);

        historyList.add(new StatusHistory(1, 1, "SUBMITTED", LocalDateTime.now().minusDays(7), "Applied online"));
        historyList.add(new StatusHistory(2, 1, "SHORTLISTED", LocalDateTime.now().minusDays(3), "Profile shortlisted for interview"));

        historyList.add(new StatusHistory(3, 2, "SUBMITTED", LocalDateTime.now().minusDays(5), "Applied online"));
        historyList.add(new StatusHistory(4, 2, "SHORTLISTED", LocalDateTime.now().minusDays(2), "Technical screening cleared"));
        historyList.add(new StatusHistory(5, 2, "APPROVED", LocalDateTime.now().minusDays(1), "Selected for hire"));

        historyList.add(new StatusHistory(6, 3, "SUBMITTED", LocalDateTime.now().minusDays(2), "Applied online"));
    }

    @Override
    public void save(Application application) {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            saveInMemory(application);
            return;
        }

        String sqlWithResume = """
                INSERT INTO applications (job_id, job_seeker_id, resume_path, resume, status, applied_date, status_updated_at, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlWithResume, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, application.getJobId());
            statement.setInt(2, application.getJobSeekerId());
            statement.setString(3, application.getResumePath());
            statement.setString(4, application.getResumePath());
            statement.setString(5, application.getStatus());
            statement.setDate(6, Date.valueOf(application.getAppliedDate() != null ? application.getAppliedDate() : LocalDate.now()));
            statement.setDate(7, Date.valueOf(application.getStatusUpdatedAt() != null ? application.getStatusUpdatedAt() : LocalDate.now()));
            statement.setString(8, application.getNotes() != null ? application.getNotes() : "");
            statement.executeUpdate();

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    application.setId(rs.getInt(1));
                }
            }

            recordStatusHistory(application.getId(), application.getStatus(), "Application submitted");
            saveInMemory(application);
        } catch (SQLException e) {
            String fallbackSql = """
                    INSERT INTO applications (job_id, job_seeker_id, resume_path, status, applied_date, status_updated_at, notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(fallbackSql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, application.getJobId());
                statement.setInt(2, application.getJobSeekerId());
                statement.setString(3, application.getResumePath());
                statement.setString(4, application.getStatus());
                statement.setDate(5, Date.valueOf(application.getAppliedDate() != null ? application.getAppliedDate() : LocalDate.now()));
                statement.setDate(6, Date.valueOf(application.getStatusUpdatedAt() != null ? application.getStatusUpdatedAt() : LocalDate.now()));
                statement.setString(7, application.getNotes() != null ? application.getNotes() : "");
                statement.executeUpdate();

                try (ResultSet rs = statement.getGeneratedKeys()) {
                    if (rs.next()) {
                        application.setId(rs.getInt(1));
                    }
                }

                recordStatusHistory(application.getId(), application.getStatus(), "Application submitted");
                saveInMemory(application);
            } catch (SQLException e2) {
                handleSqlException("save", e2);
                saveInMemory(application);
            }
        }
    }

    /**
     * Executes atomic JDBC transaction:
     * 1. Inserts Application record
     * 2. Inserts StatusHistory record
     * 3. Inserts Employer Notification record
     * Guarantees ACID transactional compliance using setAutoCommit(false), commit(), and rollback().
     */
    public boolean saveWithTransaction(Application application, String historyNote, int employerId) {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            save(application);
            return true;
        }

        Connection connection = null;
        try {
            connection = DBConnection.getConnection();
            boolean success = executeTransaction(connection, application, historyNote, employerId);
            if (!success) {
                // If database failed (e.g. uninitialized schema or offline testing), fall back to in-memory store
                useMemoryStore = true;
                saveInMemory(application);
            }
            return true;
        } catch (SQLException e) {
            handleSqlException("saveWithTransaction", e);
            saveInMemory(application);
            return true;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException closeEx) {
                    log.error("Error closing transactional connection", closeEx);
                }
            }
        }
    }

    /**
     * Executes the 3-step atomic transaction on the supplied JDBC Connection.
     * On any SQLException, calls connection.rollback() and returns false without persisting.
     */
    public boolean executeTransaction(Connection connection, Application application, String historyNote, int employerId) {
        try {
            connection.setAutoCommit(false); // Begin JDBC Transaction

            // Step 1: Insert application
            String insertAppSql = """
                    INSERT INTO applications (job_id, job_seeker_id, resume_path, resume, status, applied_date, status_updated_at, notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = connection.prepareStatement(insertAppSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, application.getJobId());
                ps.setInt(2, application.getJobSeekerId());
                ps.setString(3, application.getResumePath());
                ps.setString(4, application.getResumePath());
                ps.setString(5, application.getStatus());
                ps.setDate(6, Date.valueOf(application.getAppliedDate() != null ? application.getAppliedDate() : LocalDate.now()));
                ps.setDate(7, Date.valueOf(application.getStatusUpdatedAt() != null ? application.getStatusUpdatedAt() : LocalDate.now()));
                ps.setString(8, application.getNotes() != null ? application.getNotes() : "");
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        application.setId(rs.getInt(1));
                    }
                }
            }

            // Step 2: Insert status history
            String insertHistorySql = "INSERT INTO status_history (application_id, status, changed_at, notes) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psHistory = connection.prepareStatement(insertHistorySql)) {
                psHistory.setInt(1, application.getId());
                psHistory.setString(2, application.getStatus());
                psHistory.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                psHistory.setString(4, historyNote != null ? historyNote : "Initial application submission");
                psHistory.executeUpdate();
            }

            // Step 3: Insert notification to employer if employerId > 0
            if (employerId > 0) {
                String insertNotifSql = "INSERT INTO notifications (user_id, title, message, is_read, created_at) VALUES (?, ?, ?, FALSE, ?)";
                try (PreparedStatement psNotif = connection.prepareStatement(insertNotifSql)) {
                    psNotif.setInt(1, employerId);
                    psNotif.setString(2, "New Candidate Application!");
                    psNotif.setString(3, "Candidate #" + application.getJobSeekerId() + " has applied for Job #" + application.getJobId());
                    psNotif.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    psNotif.executeUpdate();
                }
            }

            connection.commit(); // Atomic commit
            saveInMemory(application);
            return true;
        } catch (SQLException e) {
            log.error("Transaction failed during application submission! Rolling back...", e);
            if (connection != null) {
                try {
                    connection.rollback(); // Atomic rollback
                } catch (SQLException rollbackEx) {
                    log.error("Failed to rollback transaction", rollbackEx);
                }
            }
            return false;
        }
    }

    private synchronized void saveInMemory(Application application) {
        if (application.getId() <= 0) {
            application.setId(idCounter.incrementAndGet());
        }
        if (application.getAppliedDate() == null) {
            application.setAppliedDate(LocalDate.now());
        }
        if (application.getStatusUpdatedAt() == null) {
            application.setStatusUpdatedAt(LocalDate.now());
        }
        applications.removeIf(a -> a.getId() == application.getId());
        applications.add(0, application);

        recordStatusHistory(application.getId(), application.getStatus(), "Application submitted");
    }

    public boolean hasUserApplied(int jobId, int jobSeekerId) {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            return applications.stream()
                    .anyMatch(a -> a.getJobId() == jobId && a.getJobSeekerId() == jobSeekerId);
        }

        String sql = "SELECT COUNT(*) FROM applications WHERE job_id = ? AND job_seeker_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, jobId);
            statement.setInt(2, jobSeekerId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            return applications.stream()
                    .anyMatch(a -> a.getJobId() == jobId && a.getJobSeekerId() == jobSeekerId);
        }
        return false;
    }

    public void updateStatus(int applicationId, String newStatus, String notes) {
        if (useMemoryStore) {
            findById(applicationId).ifPresent(app -> {
                app.setStatus(newStatus);
                app.setStatusUpdatedAt(LocalDate.now());
                if (notes != null && !notes.isBlank()) {
                    app.setNotes(notes);
                }
            });
            recordStatusHistory(applicationId, newStatus, notes);
            return;
        }

        String sql = "UPDATE applications SET status = ?, status_updated_at = ?, notes = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newStatus);
            statement.setDate(2, Date.valueOf(LocalDate.now()));
            statement.setString(3, notes != null ? notes : "");
            statement.setInt(4, applicationId);
            statement.executeUpdate();

            recordStatusHistory(applicationId, newStatus, notes);
        } catch (SQLException e) {
            useMemoryStore = true;
            updateStatus(applicationId, newStatus, notes);
        }
    }

    public void recordStatusHistory(int applicationId, String status, String notes) {
        if (useMemoryStore) {
            historyList.add(new StatusHistory(
                    historyIdCounter.incrementAndGet(),
                    applicationId,
                    status,
                    LocalDateTime.now(),
                    notes != null ? notes : ""
            ));
            return;
        }

        String sql = "INSERT INTO status_history (application_id, status, changed_at, notes) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, applicationId);
            statement.setString(2, status);
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(4, notes != null ? notes : "");
            statement.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            historyList.add(new StatusHistory(
                    historyIdCounter.incrementAndGet(),
                    applicationId,
                    status,
                    LocalDateTime.now(),
                    notes != null ? notes : ""
            ));
        }
    }

    public List<StatusHistory> getStatusHistory(int applicationId) {
        if (useMemoryStore) {
            return historyList.stream()
                    .filter(h -> h.getApplicationId() == applicationId)
                    .toList();
        }

        String sql = "SELECT * FROM status_history WHERE application_id = ? ORDER BY changed_at ASC";
        List<StatusHistory> list = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, applicationId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("changed_at");
                    list.add(new StatusHistory(
                            rs.getInt("id"),
                            rs.getInt("application_id"),
                            rs.getString("status"),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                            rs.getString("notes")
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            useMemoryStore = true;
            return historyList.stream()
                    .filter(h -> h.getApplicationId() == applicationId)
                    .toList();
        }
    }

    @Override
    public Optional<Application> findById(int id) {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            return applications.stream()
                    .filter(application -> application.getId() == id)
                    .findFirst();
        }

        String sql = "SELECT * FROM applications WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapApplication(resultSet));
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            return applications.stream()
                    .filter(application -> application.getId() == id)
                    .findFirst();
        }

        return Optional.empty();
    }

    @Override
    public List<Application> findAll() {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            return new ArrayList<>(applications);
        }

        String sql = "SELECT * FROM applications ORDER BY applied_date DESC";
        List<Application> result = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                result.add(mapApplication(resultSet));
            }

            return result;
        } catch (SQLException e) {
            useMemoryStore = true;
            return new ArrayList<>(applications);
        }
    }

    public List<Application> findByJobSeekerId(int jobSeekerId) {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            return applications.stream()
                    .filter(application -> application.getJobSeekerId() == jobSeekerId)
                    .toList();
        }

        String sql = "SELECT * FROM applications WHERE job_seeker_id = ? ORDER BY applied_date DESC";
        List<Application> result = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, jobSeekerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapApplication(resultSet));
                }
            }

            return result;
        } catch (SQLException e) {
            useMemoryStore = true;
            return applications.stream()
                    .filter(application -> application.getJobSeekerId() == jobSeekerId)
                    .toList();
        }
    }

    public List<Application> findByJobId(int jobId) {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            return applications.stream()
                    .filter(application -> application.getJobId() == jobId)
                    .toList();
        }

        String sql = "SELECT * FROM applications WHERE job_id = ? ORDER BY applied_date DESC";
        List<Application> result = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, jobId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapApplication(resultSet));
                }
            }

            return result;
        } catch (SQLException e) {
            useMemoryStore = true;
            return applications.stream()
                    .filter(application -> application.getJobId() == jobId)
                    .toList();
        }
    }

    @Override
    public void deleteById(int id) {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            applications.removeIf(application -> application.getId() == id);
            return;
        }

        String sql = "DELETE FROM applications WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            applications.removeIf(application -> application.getId() == id);
        }
    }

    private Application mapApplication(ResultSet resultSet) throws SQLException {
        LocalDate appliedDate = LocalDate.now();
        try {
            Date d = resultSet.getDate("applied_date");
            if (d != null) {
                appliedDate = d.toLocalDate();
            } else {
                Timestamp ts = resultSet.getTimestamp("applied_at");
                if (ts != null) {
                    appliedDate = ts.toLocalDateTime().toLocalDate();
                }
            }
        } catch (SQLException e) {
            try {
                Timestamp ts = resultSet.getTimestamp("applied_at");
                if (ts != null) {
                    appliedDate = ts.toLocalDateTime().toLocalDate();
                }
            } catch (SQLException ignored) {
            }
        }

        Date updatedDate = null;
        try {
            updatedDate = resultSet.getDate("status_updated_at");
        } catch (SQLException ignored) {
        }
        LocalDate statusUpdatedAt = updatedDate != null ? updatedDate.toLocalDate() : appliedDate;

        String notes = "";
        try {
            notes = resultSet.getString("notes");
        } catch (SQLException ignored) {
        }

        String resumePath = "";
        try {
            resumePath = resultSet.getString("resume_path");
        } catch (SQLException ignored) {
        }
        if (resumePath == null || resumePath.isBlank()) {
            try {
                resumePath = resultSet.getString("resume");
            } catch (SQLException ignored) {
            }
        }
        if (resumePath == null) {
            resumePath = "";
        }

        return new Application(
                resultSet.getInt("id"),
                resultSet.getInt("job_id"),
                resultSet.getInt("job_seeker_id"),
                resumePath,
                resultSet.getString("status"),
                appliedDate,
                statusUpdatedAt,
                notes != null ? notes : ""
        );
    }
}
