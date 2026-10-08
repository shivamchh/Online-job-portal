package com.example.Online_job_portal.controller;

import com.example.Online_job_portal.dao.NotificationDAO;
import com.example.Online_job_portal.model.Application;
import com.example.Online_job_portal.model.Job;
import com.example.Online_job_portal.model.Message;
import com.example.Online_job_portal.model.User;
import com.example.Online_job_portal.service.ApplicationService;
import com.example.Online_job_portal.service.JobService;
import com.example.Online_job_portal.service.MessageService;
import com.example.Online_job_portal.service.UserService;
import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.ValidationUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/employer")
public class EmployerController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final NotificationDAO notificationDAO;
    private final ActivityLogger activityLogger;
    private final MessageService messageService;
    private final UserService userService;

    public EmployerController(JobService jobService,
                              ApplicationService applicationService,
                              NotificationDAO notificationDAO,
                              ActivityLogger activityLogger,
                              MessageService messageService,
                              UserService userService) {
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.notificationDAO = notificationDAO;
        this.activityLogger = activityLogger;
        this.messageService = messageService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String success,
                            @RequestParam(required = false) String error,
                            Model model, HttpSession session) {
        int employerId = getSessionUserId(session);
        List<Job> myJobs = jobService.getJobsByEmployer(employerId);
        List<Application> myApplications = applicationService.getApplicationsByEmployer(employerId, jobService);

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);

        List<User> candidates = new ArrayList<>();
        for (Application app : myApplications) {
            Optional<User> u = userService.findUserById(app.getJobSeekerId());
            if (u.isPresent() && candidates.stream().noneMatch(c -> c.getId() == u.get().getId())) {
                candidates.add(u.get());
            }
        }
        if (candidates.isEmpty()) {
            userService.findUserById(3).ifPresent(candidates::add);
        }
        model.addAttribute("candidates", candidates);

        model.addAttribute("jobs", myJobs);
        model.addAttribute("applications", myApplications);
        model.addAttribute("totalJobsCount", myJobs.size());
        model.addAttribute("totalApplicationsCount", myApplications.size());
        model.addAttribute("unreadCount", notificationDAO != null ? notificationDAO.getUnreadCount(employerId) : 0);
        model.addAttribute("unreadMessagesCount", messageService != null ? messageService.getUnreadCount(employerId) : 0);

        return "employer/dashboard";
    }

    @GetMapping("/post-job")
    public String postJob(@RequestParam(required = false) String error,
                          @RequestParam(required = false) String success,
                          Model model, HttpSession session) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        return "employer/post-job";
    }

    @GetMapping("/my-jobs")
    public String myJobs(@RequestParam(required = false) String success,
                         @RequestParam(required = false) String error,
                         Model model, HttpSession session) {
        int employerId = getSessionUserId(session);
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("jobs", jobService.getJobsByEmployer(employerId));
        return "employer/my-jobs";
    }

    @GetMapping("/applications")
    public String applications(@RequestParam(required = false) String success,
                               @RequestParam(required = false) String error,
                               Model model, HttpSession session) {
        int employerId = getSessionUserId(session);
        List<Application> apps = applicationService.getApplicationsByEmployer(employerId, jobService);

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("applications", apps);
        return "employer/applications";
    }

    @PostMapping("/applications/update-status")
    public String updateApplicationStatus(@RequestParam int applicationId,
                                          @RequestParam String status,
                                          @RequestParam(required = false) String notes,
                                          HttpSession session) {
        String reviewerName = (String) session.getAttribute("userName");
        try {
            applicationService.updateApplicationStatus(applicationId, status, notes, reviewerName);
            return "redirect:/employer/applications?success=" + encode("Application #" + applicationId + " updated to " + status);
        } catch (Exception e) {
            return "redirect:/employer/applications?error=" + encode(e.getMessage());
        }
    }

    @GetMapping("/messages")
    public String messages(@RequestParam(required = false) Integer withUser,
                           @RequestParam(required = false) String success,
                           @RequestParam(required = false) String error,
                           Model model, HttpSession session) {
        int employerId = getSessionUserId(session);
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);

        // Fetch candidate users who applied to this employer's jobs
        List<Application> apps = applicationService.getApplicationsByEmployer(employerId, jobService);
        List<User> candidates = new ArrayList<>();
        for (Application app : apps) {
            Optional<User> u = userService.findUserById(app.getJobSeekerId());
            if (u.isPresent() && candidates.stream().noneMatch(c -> c.getId() == u.get().getId())) {
                candidates.add(u.get());
            }
        }
        // Fallback demo candidate if empty
        if (candidates.isEmpty()) {
            userService.findUserById(3).ifPresent(candidates::add);
        }

        int activeCandidateId = (withUser != null) ? withUser : (!candidates.isEmpty() ? candidates.get(0).getId() : 3);
        User activeCandidate = userService.findUserById(activeCandidateId).orElse(null);

        List<Message> conversation = messageService != null
                ? messageService.getConversation(employerId, activeCandidateId)
                : List.of();

        model.addAttribute("candidates", candidates);
        model.addAttribute("selectedUser", activeCandidate);
        model.addAttribute("conversation", conversation);
        model.addAttribute("currentUserId", employerId);

        return "employer/messages";
    }

    @GetMapping("/statistics")
    public String statistics(Model model, HttpSession session) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        return "employer/statistics";
    }

    private int getSessionUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        return userId instanceof Integer ? (Integer) userId : 0;
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
