package com.example.Online_job_portal.dao;

import com.example.Online_job_portal.config.DBConnection;
import com.example.Online_job_portal.model.Message;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class MessageDAO implements DAO<Message> {

    private final List<Message> messages = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(10);
    private boolean useMemoryStore;

    public MessageDAO() {
    }

    public synchronized void seedInitialMessages() {
        if (!messages.isEmpty()) return;
        messages.add(new Message(1, 2, 3, "TechCorp Solutions", "Alex Kumar",
                "Hello Alex, we reviewed your application for Senior Java Developer and were impressed. Are you available for a technical interview this Thursday?",
                LocalDateTime.now().minusDays(2), true));

        messages.add(new Message(2, 3, 2, "Alex Kumar", "TechCorp Solutions",
                "Thank you for the update! Yes, I am available this Thursday anytime after 2 PM.",
                LocalDateTime.now().minusDays(1), false));
    }

    @Override
    public void save(Message message) {
        if (useMemoryStore) {
            saveInMemory(message);
            return;
        }

        String sql = "INSERT INTO messages (sender_id, receiver_id, content, sent_at, is_read) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, message.getSenderId());
            stmt.setInt(2, message.getReceiverId());
            stmt.setString(3, message.getContent());
            stmt.setTimestamp(4, Timestamp.valueOf(message.getSentAt() != null ? message.getSentAt() : LocalDateTime.now()));
            stmt.setBoolean(5, message.isRead());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            saveInMemory(message);
        }
    }

    private synchronized void saveInMemory(Message message) {
        if (message.getId() <= 0) {
            message.setId(idCounter.incrementAndGet());
        }
        if (message.getSentAt() == null) {
            message.setSentAt(LocalDateTime.now());
        }
        messages.add(message);
    }

    public List<Message> findConversation(int user1Id, int user2Id) {
        if (useMemoryStore) {
            return messages.stream()
                    .filter(m -> (m.getSenderId() == user1Id && m.getReceiverId() == user2Id) ||
                                 (m.getSenderId() == user2Id && m.getReceiverId() == user1Id))
                    .sorted((a, b) -> a.getSentAt().compareTo(b.getSentAt()))
                    .toList();
        }

        String sql = """
                SELECT m.*, u1.name AS sender_name, u2.name AS receiver_name
                FROM messages m
                LEFT JOIN users u1 ON m.sender_id = u1.id
                LEFT JOIN users u2 ON m.receiver_id = u2.id
                WHERE (m.sender_id = ? AND m.receiver_id = ?)
                   OR (m.sender_id = ? AND m.receiver_id = ?)
                ORDER BY m.sent_at ASC
                """;

        List<Message> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user1Id);
            stmt.setInt(2, user2Id);
            stmt.setInt(3, user2Id);
            stmt.setInt(4, user1Id);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMessage(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            useMemoryStore = true;
            return findConversation(user1Id, user2Id);
        }
    }

    public List<Message> findByUser(int userId) {
        if (useMemoryStore) {
            return messages.stream()
                    .filter(m -> m.getSenderId() == userId || m.getReceiverId() == userId)
                    .sorted((a, b) -> b.getSentAt().compareTo(a.getSentAt()))
                    .toList();
        }

        String sql = """
                SELECT m.*, u1.name AS sender_name, u2.name AS receiver_name
                FROM messages m
                LEFT JOIN users u1 ON m.sender_id = u1.id
                LEFT JOIN users u2 ON m.receiver_id = u2.id
                WHERE m.sender_id = ? OR m.receiver_id = ?
                ORDER BY m.sent_at DESC
                """;

        List<Message> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMessage(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            useMemoryStore = true;
            return findByUser(userId);
        }
    }

    public int getUnreadCount(int userId) {
        if (useMemoryStore) {
            return (int) messages.stream()
                    .filter(m -> m.getReceiverId() == userId && !m.isRead())
                    .count();
        }

        String sql = "SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND is_read = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            return getUnreadCount(userId);
        }
        return 0;
    }

    public void markAsRead(int messageId) {
        if (useMemoryStore) {
            findById(messageId).ifPresent(m -> m.setRead(true));
            return;
        }

        String sql = "UPDATE messages SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            markAsRead(messageId);
        }
    }

    @Override
    public Optional<Message> findById(int id) {
        if (useMemoryStore) {
            return messages.stream().filter(m -> m.getId() == id).findFirst();
        }

        String sql = """
                SELECT m.*, u1.name AS sender_name, u2.name AS receiver_name
                FROM messages m
                LEFT JOIN users u1 ON m.sender_id = u1.id
                LEFT JOIN users u2 ON m.receiver_id = u2.id
                WHERE m.id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapMessage(rs));
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            return messages.stream().filter(m -> m.getId() == id).findFirst();
        }
        return Optional.empty();
    }

    @Override
    public List<Message> findAll() {
        if (useMemoryStore) {
            return new ArrayList<>(messages);
        }

        String sql = """
                SELECT m.*, u1.name AS sender_name, u2.name AS receiver_name
                FROM messages m
                LEFT JOIN users u1 ON m.sender_id = u1.id
                LEFT JOIN users u2 ON m.receiver_id = u2.id
                ORDER BY m.sent_at DESC
                """;

        List<Message> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapMessage(rs));
            }
            return list;
        } catch (SQLException e) {
            useMemoryStore = true;
            return new ArrayList<>(messages);
        }
    }

    @Override
    public void deleteById(int id) {
        if (useMemoryStore) {
            messages.removeIf(m -> m.getId() == id);
            return;
        }

        String sql = "DELETE FROM messages WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            messages.removeIf(m -> m.getId() == id);
        }
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("sent_at");
        String senderName = "";
        String receiverName = "";
        try { senderName = rs.getString("sender_name"); } catch (SQLException ignored) {}
        try { receiverName = rs.getString("receiver_name"); } catch (SQLException ignored) {}
        boolean read = false;
        try { read = rs.getBoolean("is_read"); } catch (SQLException ignored) {}

        return new Message(
                rs.getInt("id"),
                rs.getInt("sender_id"),
                rs.getInt("receiver_id"),
                senderName != null ? senderName : "",
                receiverName != null ? receiverName : "",
                rs.getString("content"),
                ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                read
        );
    }
}
