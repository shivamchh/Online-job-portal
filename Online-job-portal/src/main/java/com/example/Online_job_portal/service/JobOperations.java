package com.example.Online_job_portal.service;

import com.example.Online_job_portal.model.Job;

import java.util.List;

public interface JobOperations {

    void postJob(Job job);

    void updateJob(Job job);

    void deleteJob(int jobId);

    List<Job> getAllJobs();
}