package com.example.Online_job_portal.dao;

import com.example.Online_job_portal.config.DBConnection;
import com.example.Online_job_portal.model.Application;
import com.example.Online_job_portal.model.StatusHistory;
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

    private final List<Application> applications = new ArrayList<>();
    private final List<StatusHistory> historyList = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(10);
    private final AtomicInteger historyIdCounter = new AtomicInteger(10);
    private boolean useMemoryStore;

    public ApplicationDAO() {
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

        String sql = """
                INSERT INTO applications (job_id, job_seeker_id, resume_path, status, applied_date, status_updated_at, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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
        } catch (SQLException e) {
            useMemoryStore = true;
            saveInMemory(application);
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
        if (useMemoryStore) {
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
        if (useMemoryStore) {
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
        LocalDate appliedDate = resultSet.getDate("applied_date").toLocalDate();
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

        return new Application(
                resultSet.getInt("id"),
                resultSet.getInt("job_id"),
                resultSet.getInt("job_seeker_id"),
                resultSet.getString("resume_path"),
                resultSet.getString("status"),
                appliedDate,
                statusUpdatedAt,
                notes != null ? notes : ""
        );
    }
}
