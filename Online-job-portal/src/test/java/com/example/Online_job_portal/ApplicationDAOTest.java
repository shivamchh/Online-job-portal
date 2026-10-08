package com.example.Online_job_portal;

import com.example.Online_job_portal.dao.ApplicationDAO;
import com.example.Online_job_portal.model.Application;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationDAOTest {

    @Test
    void savesApplication() {
        ApplicationDAO applicationDAO = new ApplicationDAO();

        applicationDAO.save(new Application(1, 1, 3, "resume.pdf", "SUBMITTED", LocalDate.now()));

        assertTrue(applicationDAO.findById(1).isPresent());
    }
}

