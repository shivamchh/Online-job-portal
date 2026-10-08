package com.example.Online_job_portal.dao;

import com.example.Online_job_portal.config.DBConnection;
import com.example.Online_job_portal.model.Admin;
import com.example.Online_job_portal.model.Employer;
import com.example.Online_job_portal.model.JobSeeker;
import com.example.Online_job_portal.model.User;
import com.example.Online_job_portal.util.PasswordUtil;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class UserDAO implements DAO<User> {

    private final List<User> users = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(10);
    private boolean useMemoryStore;

    public UserDAO() {
    }

    public synchronized void seedInitialUsers() {
        if (!users.isEmpty()) return;
        String saltAdmin = PasswordUtil.generateSalt();
        String saltEmployer = PasswordUtil.generateSalt();
        String saltSeeker = PasswordUtil.generateSalt();
        String saltPriya = PasswordUtil.generateSalt();

        users.add(new Admin(1, "Admin User", "admin@portal.com", PasswordUtil.hashPassword("admin123", saltAdmin), saltAdmin, "9876543210", "ACTIVE", "", ""));
        users.add(new Employer(2, "TechCorp Solutions", "employer@techcorp.com", PasswordUtil.hashPassword("employer123", saltEmployer), saltEmployer, "9876543211", "ACTIVE", "", ""));
        users.add(new JobSeeker(3, "Alex Kumar", "seeker@example.com", PasswordUtil.hashPassword("seeker123", saltSeeker), saltSeeker, "9876543212", "ACTIVE", "Java, Spring Boot, MySQL, REST API", "alex_resume.pdf"));
        users.add(new JobSeeker(4, "Priya Sharma", "priya@example.com", PasswordUtil.hashPassword("priya123", saltPriya), saltPriya, "9876543213", "ACTIVE", "Python, Django, React, AWS", "priya_resume.pdf"));
    }

    @Override
    public void save(User user) {
        if (useMemoryStore) {
            saveInMemory(user);
            return;
        }

        String sql = "INSERT INTO users (name, email, password, salt, phone, role, status, skills, default_resume_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getSalt() != null ? user.getSalt() : "");
            statement.setString(5, user.getPhone());
            statement.setString(6, getRole(user));
            statement.setString(7, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            statement.setString(8, user.getSkills() != null ? user.getSkills() : "");
            statement.setString(9, user.getDefaultResumePath() != null ? user.getDefaultResumePath() : "");
            statement.executeUpdate();

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            saveInMemory(user);
        }
    }

    private synchronized void saveInMemory(User user) {
        if (user.getId() <= 0) {
            user.setId(idCounter.incrementAndGet());
        }
        users.removeIf(u -> u.getId() == user.getId() || u.getEmail().equalsIgnoreCase(user.getEmail()));
        users.add(user);
    }

    @Override
    public Optional<User> findById(int id) {
        if (useMemoryStore) {
            return users.stream()
                    .filter(user -> user.getId() == id)
                    .findFirst();
        }

        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            return users.stream()
                    .filter(user -> user.getId() == id)
                    .findFirst();
        }

        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        if (useMemoryStore) {
            return users.stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        }

        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            return users.stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        }

        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        if (useMemoryStore) {
            return new ArrayList<>(users);
        }

        String sql = "SELECT * FROM users ORDER BY id";
        List<User> result = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                result.add(mapUser(resultSet));
            }

            return result;
        } catch (SQLException e) {
            useMemoryStore = true;
            return new ArrayList<>(users);
        }
    }

    public List<User> findAllActive() {
        return findAll().stream()
                .filter(User::isActive)
                .toList();
    }

    public void updateSkills(int userId, String skills) {
        if (useMemoryStore) {
            findById(userId).ifPresent(u -> u.setSkills(skills));
            return;
        }

        String sql = "UPDATE users SET skills = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, skills);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            findById(userId).ifPresent(u -> u.setSkills(skills));
        }
    }

    public void updateProfile(int userId, String name, String email, String phone, String skills, String defaultResumePath) {
        if (useMemoryStore) {
            findById(userId).ifPresent(u -> {
                u.setName(name);
                u.setEmail(email);
                u.setPhone(phone);
                u.setSkills(skills);
                if (defaultResumePath != null && !defaultResumePath.isBlank()) {
                    u.setDefaultResumePath(defaultResumePath);
                }
            });
            return;
        }

        boolean hasResume = defaultResumePath != null && !defaultResumePath.isBlank();
        String sql = hasResume
                ? "UPDATE users SET name = ?, email = ?, phone = ?, skills = ?, default_resume_path = ? WHERE id = ?"
                : "UPDATE users SET name = ?, email = ?, phone = ?, skills = ? WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.setString(4, skills);
            if (hasResume) {
                statement.setString(5, defaultResumePath);
                statement.setInt(6, userId);
            } else {
                statement.setInt(5, userId);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            findById(userId).ifPresent(u -> {
                u.setName(name);
                u.setEmail(email);
                u.setPhone(phone);
                u.setSkills(skills);
                if (defaultResumePath != null && !defaultResumePath.isBlank()) {
                    u.setDefaultResumePath(defaultResumePath);
                }
            });
        }
    }

    public void updateResumePath(int userId, String resumePath) {
        if (useMemoryStore) {
            findById(userId).ifPresent(u -> u.setDefaultResumePath(resumePath));
            return;
        }

        String sql = "UPDATE users SET default_resume_path = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, resumePath);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            findById(userId).ifPresent(u -> u.setDefaultResumePath(resumePath));
        }
    }

    public void toggleUserStatus(int id) {
        Optional<User> opt = findById(id);
        if (opt.isPresent()) {
            User u = opt.get();
            String newStatus = "ACTIVE".equalsIgnoreCase(u.getStatus()) ? "INACTIVE" : "ACTIVE";
            setUserStatus(id, newStatus);
        }
    }

    public void setUserStatus(int id, String status) {
        if (useMemoryStore) {
            findById(id).ifPresent(u -> u.setStatus(status));
            return;
        }

        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            findById(id).ifPresent(u -> u.setStatus(status));
        }
    }

    @Override
    public void deleteById(int id) {
        // Soft delete: marks as INACTIVE rather than destroying rows
        setUserStatus(id, "INACTIVE");
    }

    public void hardDeleteById(int id) {
        if (useMemoryStore) {
            users.removeIf(user -> user.getId() == id);
            return;
        }

        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            users.removeIf(user -> user.getId() == id);
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String name = resultSet.getString("name");
        String email = resultSet.getString("email");
        String password = resultSet.getString("password");
        String phone = resultSet.getString("phone");
        String role = resultSet.getString("role");

        String salt = "";
        try {
            salt = resultSet.getString("salt");
        } catch (SQLException ignored) {
        }
        if (salt == null) {
            salt = "";
        }

        String defaultResumePath = "";
        try {
            defaultResumePath = resultSet.getString("default_resume_path");
        } catch (SQLException ignored) {
        }
        if (defaultResumePath == null) {
            defaultResumePath = "";
        }

        String status = "ACTIVE";
        try {
            status = resultSet.getString("status");
        } catch (SQLException ignored) {
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }

        String skills = "";
        try {
            skills = resultSet.getString("skills");
        } catch (SQLException ignored) {
        }
        if (skills == null) {
            skills = "";
        }

        if ("ADMIN".equalsIgnoreCase(role)) {
            return new Admin(id, name, email, password, salt, phone, status, skills, defaultResumePath);
        }

        if ("EMPLOYER".equalsIgnoreCase(role)) {
            return new Employer(id, name, email, password, salt, phone, status, skills, defaultResumePath);
        }

        return new JobSeeker(id, name, email, password, salt, phone, status, skills, defaultResumePath);
    }

    public String getRole(User user) {
        if (user instanceof Admin) {
            return "ADMIN";
        }

        if (user instanceof Employer) {
            return "EMPLOYER";
        }

        return "JOB_SEEKER";
    }
}
