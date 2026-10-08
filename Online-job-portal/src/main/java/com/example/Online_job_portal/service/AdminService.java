package com.example.Online_job_portal.service;

import com.example.Online_job_portal.dao.ActivityLogDAO;
import com.example.Online_job_portal.dao.SettingsDAO;
import com.example.Online_job_portal.model.ActivityLog;
import com.example.Online_job_portal.model.Job;
import com.example.Online_job_portal.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final ActivityLogDAO activityLogDAO;
    private final SettingsDAO settingsDAO;

    public AdminService(UserService userService,
                        JobService jobService,
                        ApplicationService applicationService,
                        ActivityLogDAO activityLogDAO,
                        SettingsDAO settingsDAO) {
        this.userService = userService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.activityLogDAO = activityLogDAO;
        this.settingsDAO = settingsDAO;
    }

    public Map<String, String> getAllSettings() {
        return settingsDAO.getAll();
    }

    public String getSetting(String key, String defaultValue) {
        return settingsDAO.getValue(key, defaultValue);
    }

    public void updateSetting(String key, String value) {
        settingsDAO.setValue(key, value);
    }

    public List<com.example.Online_job_portal.model.Setting> findAllSettings() {
        return settingsDAO.findAllSettings();
    }

    public List<User> viewUsers() {
        return userService.getAllUsers();
    }

    public List<Job> viewJobs() {
        return jobService.getAllJobs();
    }

    public int countJobs() {
        return jobService.getAllJobs().size();
    }

    public int countActiveJobs() {
        return jobService.getAllActiveJobs().size();
    }

    public int countUsers() {
        return userService.getAllUsers().size();
    }

    public int countApplications() {
        return applicationService.getAllApplications().size();
    }

    public List<ActivityLog> getRecentActivities(int limit) {
        return activityLogDAO.findLatest(limit);
    }

    public void toggleUserStatus(int userId) {
        userService.toggleUserStatus(userId);
    }

    public void toggleJobStatus(int jobId) {
        jobService.toggleJobStatus(jobId);
    }
}
