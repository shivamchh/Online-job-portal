package com.example.Online_job_portal.config;

import com.example.Online_job_portal.dao.ApplicationDAO;
import com.example.Online_job_portal.dao.JobDAO;
import com.example.Online_job_portal.dao.UserDAO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes realistic demo dataset on application startup for examiners and demo showcases.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserDAO userDAO;
    private final JobDAO jobDAO;
    private final ApplicationDAO applicationDAO;

    public DataInitializer(UserDAO userDAO, JobDAO jobDAO, ApplicationDAO applicationDAO) {
        this.userDAO = userDAO;
        this.jobDAO = jobDAO;
        this.applicationDAO = applicationDAO;
    }

    @Override
    public void run(String... args) {
        userDAO.seedInitialUsers();
        jobDAO.seedInitialJobs();
        applicationDAO.seedInitialApplications();
    }
}
