package com.example.Online_job_portal.service;

import com.example.Online_job_portal.dao.ApplicationDAO;
import com.example.Online_job_portal.dao.NotificationDAO;
import com.example.Online_job_portal.exception.ApplicationException;
import com.example.Online_job_portal.model.Application;
import com.example.Online_job_portal.model.Job;
import com.example.Online_job_portal.model.StatusHistory;
import com.example.Online_job_portal.thread.NotificationTask;
import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.ThreadManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationDAO applicationDAO;
    private final NotificationDAO notificationDAO;
    private final ThreadManager threadManager;
    private final ActivityLogger activityLogger;

    public ApplicationService(ApplicationDAO applicationDAO,
                              NotificationDAO notificationDAO,
                              ThreadManager threadManager,
                              ActivityLogger activityLogger) {
        this.applicationDAO = applicationDAO;
        this.notificationDAO = notificationDAO;
        this.threadManager = threadManager;
        this.activityLogger = activityLogger;
    }

    public boolean hasUserApplied(int jobId, int jobSeekerId) {
        return applicationDAO.hasUserApplied(jobId, jobSeekerId);
    }

    public void submitApplication(Application application) {
        applicationDAO.save(application);
    }

    public Application applyForJob(int jobId, int jobSeekerId, String resumePath, String seekerName) throws ApplicationException {
        return applyForJob(jobId, jobSeekerId, resumePath, seekerName, null);
    }

    public Application applyForJob(int jobId, int jobSeekerId, String resumePath, String seekerName, String coverLetter) throws ApplicationException {
        if (applicationDAO.hasUserApplied(jobId, jobSeekerId)) {
            throw new ApplicationException("You have already applied for this job!");
        }

        String initialNotes = (coverLetter != null && !coverLetter.isBlank())
                ? "Cover Letter: " + coverLetter.trim()
                : "Initial application submission";

        Application application = new Application(
                0,
                jobId,
                jobSeekerId,
                resumePath,
                "SUBMITTED",
                LocalDate.now(),
                LocalDate.now(),
                initialNotes
        );

        applicationDAO.save(application);

        // Multithreaded activity logging
        activityLogger.log(jobSeekerId, seekerName != null ? seekerName : "JobSeeker #" + jobSeekerId,
                "APPLICATION_SUBMITTED", "Applied for job #" + jobId);

        // Multithreaded notification task to confirm submission
        threadManager.executeTask(new NotificationTask(
                notificationDAO,
                jobSeekerId,
                "Application Received",
                "Your application for job #" + jobId + " was received successfully with status SUBMITTED."
        ));

        return application;
    }

    public void submitApplication(int jobId, int jobSeekerId, String resumePath) {
        try {
            applyForJob(jobId, jobSeekerId, resumePath, "JobSeeker");
        } catch (ApplicationException e) {
            // Keep backward-compatible no-throw for legacy callers if needed
            System.err.println("Duplicate application ignored: " + e.getMessage());
        }
    }

    public void updateApplicationStatus(int applicationId, String newStatus, String notes, String reviewerName) {
        applicationDAO.updateStatus(applicationId, newStatus, notes);

        Optional<Application> appOpt = applicationDAO.findById(applicationId);
        if (appOpt.isPresent()) {
            Application app = appOpt.get();

            // Log activity in background queue
            activityLogger.log(0, reviewerName != null ? reviewerName : "Employer",
                    "STATUS_UPDATE", "Application #" + applicationId + " updated to " + newStatus);

            // Multithreaded notification task to notify the job seeker
            threadManager.executeTask(new NotificationTask(
                    notificationDAO,
                    app.getJobSeekerId(),
                    "Application Status Updated",
                    "Your application status for job #" + app.getJobId() + " has changed to: " + newStatus
                            + (notes != null && !notes.isBlank() ? " (" + notes + ")" : "")
            ));
        }
    }

    public List<StatusHistory> getStatusHistory(int applicationId) {
        return applicationDAO.getStatusHistory(applicationId);
    }

    public Optional<Application> findApplicationById(int id) {
        return applicationDAO.findById(id);
    }

    public List<Application> getAllApplications() {
        return applicationDAO.findAll();
    }

    public List<Application> getApplicationsByJobSeeker(int jobSeekerId) {
        return applicationDAO.findByJobSeekerId(jobSeekerId);
    }

    public List<Application> getApplicationsByEmployer(int employerId, JobService jobService) {
        List<Job> employerJobs = jobService.getJobsByEmployer(employerId);
        Set<Integer> employerJobIds = employerJobs.stream().map(Job::getId).collect(Collectors.toSet());
        return applicationDAO.findAll().stream()
                .filter(a -> employerJobIds.contains(a.getJobId()))
                .toList();
    }
}
