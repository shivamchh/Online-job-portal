package com.example.Online_job_portal.test;

import com.example.Online_job_portal.exception.JobNotFoundException;
import com.example.Online_job_portal.model.Job;
import com.example.Online_job_portal.service.JobService;

public class JobServiceTest {

    public static void main(String[] args) {

        JobService jobService = new JobService();

        Job job = new Job(
                1,
                101,
                "Java Developer",
                "Develop backend applications",
                "Java, Spring Boot, MySQL",
                60000,
                "Delhi",
                "Full Time",
                "PENDING"
        );

        jobService.postJob(job);

        try {

            Job foundJob = jobService.findJobById(10);

            System.out.println(
                    "Job Found: " + foundJob.getTitle()
            );

        } catch (JobNotFoundException e) {

            System.out.println(
                    "Error: " + e.getMessage()
            );
        }
    }
}