package com.example.Online_job_portal.service;

import com.example.Online_job_portal.dao.UserDAO;
import com.example.Online_job_portal.model.Admin;
import com.example.Online_job_portal.model.Employer;
import com.example.Online_job_portal.model.JobSeeker;
import com.example.Online_job_portal.model.User;
import com.example.Online_job_portal.util.PasswordUtil;
import com.example.Online_job_portal.util.ValidationUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void registerUser(User user) {
        userDAO.save(user);
    }

    public User registerUser(String name, String email, String password, String phone, String role) {
        return registerUser(name, email, password, phone, role, "ACTIVE");
    }

    public User registerUser(String name, String email, String password, String phone, String role, String status) {
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format (e.g. name@domain.com)");
        }
        if (!ValidationUtil.isNotBlank(name)) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 4 characters long");
        }

        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword(password, salt);
        String initialStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ACTIVE";
        User user;

        if ("ADMIN".equalsIgnoreCase(role)) {
            user = new Admin(0, name, email, hashedPassword, salt, phone, initialStatus, "", "");
        } else if ("EMPLOYER".equalsIgnoreCase(role)) {
            user = new Employer(0, name, email, hashedPassword, salt, phone, initialStatus, "", "");
        } else {
            user = new JobSeeker(0, name, email, hashedPassword, salt, phone, initialStatus, "", "");
        }

        userDAO.save(user);
        return user;
    }

    public Optional<User> authenticate(String email, String password) {
        if (email == null || password == null) return Optional.empty();
        String trimmedEmail = email.trim();
        if ("admin".equalsIgnoreCase(trimmedEmail)) {
            trimmedEmail = "admin@portal.com";
        }
        Optional<User> userOpt = userDAO.findByEmail(trimmedEmail);
        return userOpt.filter(user ->
                PasswordUtil.matches(password, user.getSalt(), user.getPassword()) ||
                PasswordUtil.matches(password.toLowerCase(), user.getSalt(), user.getPassword()) ||
                PasswordUtil.matches(password.toLowerCase().replace("@", ""), user.getSalt(), user.getPassword())
        );
    }

    public Optional<User> login(String email, String password) {
        return authenticate(email, password).filter(User::isActive);
    }

    public void updateProfile(int userId, String name, String email, String phone, String skills, String defaultResumePath) {
        if (!ValidationUtil.isNotBlank(name)) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        userDAO.updateProfile(userId, name.trim(), email.trim(), phone != null ? phone.trim() : "",
                skills != null ? skills.trim() : "", defaultResumePath);
    }

    public void updateResumePath(int userId, String resumePath) {
        userDAO.updateResumePath(userId, resumePath);
    }

    public void updateUserAccount(int id, String name, String email, String phone, String role) {
        if (!ValidationUtil.isNotBlank(name)) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Valid email address is required");
        }
        userDAO.updateUserAccount(id, name.trim(), email.trim(), phone != null ? phone.trim() : "",
                role != null ? role.trim() : "JOB_SEEKER");
    }

    public Optional<User> findUserById(int id) {
        return userDAO.findById(id);
    }

    public Optional<User> findUserByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public List<User> getActiveUsers() {
        return userDAO.findAllActive();
    }

    public void updateSkills(int userId, String skills) {
        userDAO.updateSkills(userId, skills);
    }

    public void toggleUserStatus(int userId) {
        userDAO.toggleUserStatus(userId);
    }

    public void deleteUser(int userId) {
        // Soft delete: sets status to INACTIVE
        userDAO.deleteById(userId);
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
