package com.example.Online_job_portal;

import com.example.Online_job_portal.dao.UserDAO;
import com.example.Online_job_portal.model.Admin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDAOTest {

    @Test
    void savesAndFindsUser() {
        UserDAO userDAO = new UserDAO();

        userDAO.save(new Admin(1, "Admin", "admin@example.com", "admin123", "9876543210"));

        assertTrue(userDAO.findById(1).isPresent());
    }
}

