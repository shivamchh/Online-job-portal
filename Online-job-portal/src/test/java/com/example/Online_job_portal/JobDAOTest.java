package com.example.Online_job_portal;

import com.example.Online_job_portal.dao.JobDAO;
import com.example.Online_job_portal.model.Job;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobDAOTest {

    @Test
    void savesJob() {
        JobDAO jobDAO = new JobDAO();

        jobDAO.save(new Job(1, 101, "Tester", "Test applications",
                "Manual Testing", 25000, "Pune", "Full Time", "OPEN"));

        assertEquals(1, jobDAO.findAll().size());
    }
}

