package com.example.Online_job_portal.service;

import com.example.Online_job_portal.dao.JobDAO;
import com.example.Online_job_portal.exception.JobNotFoundException;
import com.example.Online_job_portal.model.Job;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobService implements JobOperations {

    private final JobDAO jobDAO;

    public JobService() {
        this.jobDAO = new JobDAO();
    }

    public JobService(JobDAO jobDAO) {
        this.jobDAO = jobDAO;
    }

    @Override
    public void postJob(Job job) {
        jobDAO.save(job);
        System.out.println("Job posted successfully.");
    }

    @Override
    public void updateJob(Job job) {
        jobDAO.setJobStatus(job.getId(), job.getStatus());
        System.out.println("Job updated successfully.");
    }

    @Override
    public void deleteJob(int jobId) {
        // Soft delete: marks job status as INACTIVE
        jobDAO.deleteById(jobId);
        System.out.println("Job deactivated successfully (soft delete).");
    }

    @Override
    public List<Job> getAllJobs() {
        return jobDAO.findAll();
    }

    public List<Job> getAllActiveJobs() {
        return jobDAO.findAllActive();
    }

    public List<Job> getJobsByEmployer(int employerId) {
        return jobDAO.findByEmployerId(employerId);
    }

    public List<Job> searchJobs(String keyword, String location, String jobType, Double minSalary, int page, int pageSize) {
        int offset = (Math.max(1, page) - 1) * pageSize;
        return jobDAO.searchJobs(keyword, location, jobType, minSalary, offset, pageSize);
    }

    public int countSearchJobs(String keyword, String location, String jobType, Double minSalary) {
        return jobDAO.countSearchJobs(keyword, location, jobType, minSalary);
    }

    public void toggleJobStatus(int jobId) {
        jobDAO.toggleJobStatus(jobId);
    }

    public void setJobStatus(int jobId, String status) {
        jobDAO.setJobStatus(jobId, status);
    }

    public Job findJobById(int jobId) throws JobNotFoundException {
        return jobDAO.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(
                        "Job with ID " + jobId + " not found."
                ));
    }
}
