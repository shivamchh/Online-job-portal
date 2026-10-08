package com.example.Online_job_portal;

import com.example.Online_job_portal.exception.JobNotFoundException;
import com.example.Online_job_portal.model.Job;
import com.example.Online_job_portal.service.JobService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobServiceTest {

    @Test
    void findsPostedJobById() throws JobNotFoundException {
        JobService jobService = new JobService();
        Job job = new Job(1, 101, "Java Developer", "Develop backend applications",
                "Java, Spring Boot, MySQL", 60000, "Delhi", "Full Time", "PENDING");

        jobService.postJob(job);

        assertEquals("Java Developer", jobService.findJobById(1).getTitle());
    }

    @Test
    void throwsExceptionWhenJobIsMissing() {
        JobService jobService = new JobService();

        assertThrows(JobNotFoundException.class, () -> jobService.findJobById(10));
    }
}

