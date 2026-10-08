package com.example.Online_job_portal.dao;

import com.example.Online_job_portal.config.DBConnection;
import com.example.Online_job_portal.model.ActivityLog;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class ActivityLogDAO implements DAO<ActivityLog> {

    private final List<ActivityLog> logs = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(100);
    private boolean useMemoryStore;

    @Override
    public void save(ActivityLog log) {
        if (useMemoryStore) {
            saveInMemory(log);
            return;
        }

        String sql = "INSERT INTO activity_logs (user_id, user_name, action, details, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, log.getUserId() > 0 ? log.getUserId() : null, Types.INTEGER);
            stmt.setString(2, log.getUserName());
            stmt.setString(3, log.getAction());
            stmt.setString(4, log.getDetails());
            stmt.setTimestamp(5, Timestamp.valueOf(log.getTimestamp() != null ? log.getTimestamp() : LocalDateTime.now()));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            saveInMemory(log);
        }
    }

    private synchronized void saveInMemory(ActivityLog log) {
        if (log.getId() <= 0) {
            log.setId(idCounter.incrementAndGet());
        }
        if (log.getTimestamp() == null) {
            log.setTimestamp(LocalDateTime.now());
        }
        logs.add(0, log);
    }

    public List<ActivityLog> findLatest(int limit) {
        if (useMemoryStore) {
            return logs.stream().limit(limit).toList();
        }

        String sql = "SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT ?";
        List<ActivityLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLog(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            useMemoryStore = true;
            return logs.stream().limit(limit).toList();
        }
    }

    @Override
    public Optional<ActivityLog> findById(int id) {
        if (useMemoryStore) {
            return logs.stream().filter(l -> l.getId() == id).findFirst();
        }

        String sql = "SELECT * FROM activity_logs WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapLog(rs));
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            return logs.stream().filter(l -> l.getId() == id).findFirst();
        }
        return Optional.empty();
    }

    @Override
    public List<ActivityLog> findAll() {
        if (useMemoryStore) {
            return new ArrayList<>(logs);
        }

        String sql = "SELECT * FROM activity_logs ORDER BY timestamp DESC";
        List<ActivityLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapLog(rs));
            }
            return list;
        } catch (SQLException e) {
            useMemoryStore = true;
            return new ArrayList<>(logs);
        }
    }

    @Override
    public void deleteById(int id) {
        if (useMemoryStore) {
            logs.removeIf(l -> l.getId() == id);
            return;
        }

        String sql = "DELETE FROM activity_logs WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            logs.removeIf(l -> l.getId() == id);
        }
    }

    private ActivityLog mapLog(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("timestamp");
        return new ActivityLog(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("user_name"),
                rs.getString("action"),
                rs.getString("details"),
                ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
        );
    }
}
