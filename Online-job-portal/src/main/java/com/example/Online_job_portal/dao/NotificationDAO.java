package com.example.Online_job_portal.dao;

import com.example.Online_job_portal.config.DBConnection;
import com.example.Online_job_portal.model.Notification;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class NotificationDAO implements DAO<Notification> {

    private final List<Notification> notifications = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(100);
    private boolean useMemoryStore;

    public NotificationDAO() {
        seedInitialNotifications();
    }

    public synchronized void seedInitialNotifications() {
        if (!notifications.isEmpty()) return;
        notifications.add(new Notification(1, 105, "🎉 Welcome to JobSphere!", "Your job seeker account is active. Explore verified open job positions and apply today.", false, LocalDateTime.now().minusHours(2)));
        notifications.add(new Notification(2, 105, "Profile Match Alert", "3 new Java & Spring Boot openings match your profile skills.", false, LocalDateTime.now().minusHours(1)));
        notifications.add(new Notification(3, 106, "Welcome to JobSphere!", "Your job seeker profile is complete. Check out recommended opportunities.", false, LocalDateTime.now().minusHours(3)));
    }

    @Override
    public void save(Notification notification) {
        if (useMemoryStore) {
            saveInMemory(notification);
            return;
        }

        String sql = "INSERT INTO notifications (user_id, title, message, is_read, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, notification.getUserId());
            stmt.setString(2, notification.getTitle());
            stmt.setString(3, notification.getMessage());
            stmt.setBoolean(4, notification.isRead());
            stmt.setTimestamp(5, Timestamp.valueOf(notification.getCreatedAt() != null ? notification.getCreatedAt() : LocalDateTime.now()));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    notification.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            saveInMemory(notification);
        }
    }

    private synchronized void saveInMemory(Notification notification) {
        if (notification.getId() <= 0) {
            notification.setId(idCounter.incrementAndGet());
        }
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        notifications.add(0, notification);
    }

    @Override
    public Optional<Notification> findById(int id) {
        if (useMemoryStore) {
            return notifications.stream().filter(n -> n.getId() == id).findFirst();
        }

        String sql = "SELECT * FROM notifications WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapNotification(rs));
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            return notifications.stream().filter(n -> n.getId() == id).findFirst();
        }
        return Optional.empty();
    }

    public List<Notification> findByUserId(int userId) {
        if (useMemoryStore) {
            return notifications.stream()
                    .filter(n -> n.getUserId() == userId)
                    .toList();
        }

        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapNotification(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            useMemoryStore = true;
            return notifications.stream()
                    .filter(n -> n.getUserId() == userId)
                    .toList();
        }
    }

    public int getUnreadCount(int userId) {
        return (int) findByUserId(userId).stream()
                .filter(n -> !n.isRead())
                .count();
    }

    public void markAsRead(int notificationId) {
        if (useMemoryStore) {
            notifications.stream()
                    .filter(n -> n.getId() == notificationId)
                    .findFirst()
                    .ifPresent(n -> n.setRead(true));
            return;
        }

        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, notificationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            markAsRead(notificationId);
        }
    }

    public void markAllAsRead(int userId) {
        if (useMemoryStore) {
            notifications.stream()
                    .filter(n -> n.getUserId() == userId)
                    .forEach(n -> n.setRead(true));
            return;
        }

        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            markAllAsRead(userId);
        }
    }

    @Override
    public List<Notification> findAll() {
        if (useMemoryStore) {
            return new ArrayList<>(notifications);
        }

        String sql = "SELECT * FROM notifications ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapNotification(rs));
            }
            return list;
        } catch (SQLException e) {
            useMemoryStore = true;
            return new ArrayList<>(notifications);
        }
    }

    @Override
    public void deleteById(int id) {
        if (useMemoryStore) {
            notifications.removeIf(n -> n.getId() == id);
            return;
        }

        String sql = "DELETE FROM notifications WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            notifications.removeIf(n -> n.getId() == id);
        }
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Notification(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getBoolean("is_read"),
                ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
        );
    }
}
