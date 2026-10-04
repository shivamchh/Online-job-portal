package com.example.Online_job_portal.service;

import com.example.Online_job_portal.exception.JobNotFoundException;
import com.example.Online_job_portal.model.Job;

import java.util.ArrayList;
import java.util.List;

public class JobService implements JobOperations {

    private List<Job> jobs = new ArrayList<>();

    @Override
    public void postJob(Job job) {

        jobs.add(job);

        System.out.println("Job posted successfully.");
    }

    @Override
    public void updateJob(Job job) {

        for (int i = 0; i < jobs.size(); i++) {

            if (jobs.get(i).getId() == job.getId()) {

                jobs.set(i, job);

                System.out.println("Job updated successfully.");

                return;
            }
        }

        System.out.println("Job not found.");
    }

    @Override
    public void deleteJob(int jobId) {

        for (int i = 0; i < jobs.size(); i++) {

            if (jobs.get(i).getId() == jobId) {

                jobs.remove(i);

                System.out.println("Job deleted successfully.");

                return;
            }
        }

        System.out.println("Job not found.");
    }

    @Override
    public List<Job> getAllJobs() {

        return jobs;
    }

    // Exception handling
    public Job findJobById(int jobId) throws JobNotFoundException {

        for (Job job : jobs) {

            if (job.getId() == jobId) {
                return job;
            }
        }

        throw new JobNotFoundException(
                "Job with ID " + jobId + " not found."
        );
    }
}