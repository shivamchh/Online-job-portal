package com.example.Online_job_portal.controller;

import com.example.Online_job_portal.dao.NotificationDAO;
import com.example.Online_job_portal.model.*;
import com.example.Online_job_portal.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
@RequestMapping("/jobseeker")
public class JobSeekerController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final RecommendationService recommendationService;
    private final UserService userService;
    private final NotificationDAO notificationDAO;
    private final MessageService messageService;

    public JobSeekerController(JobService jobService,
                               ApplicationService applicationService,
                               RecommendationService recommendationService,
                               UserService userService,
                               NotificationDAO notificationDAO,
                               MessageService messageService) {
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.recommendationService = recommendationService;
        this.userService = userService;
        this.notificationDAO = notificationDAO;
        this.messageService = messageService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String success,
                            @RequestParam(required = false) String error,
                            Model model, HttpSession session) {
        int seekerId = getSessionUserId(session);
        List<Application> myApps = applicationService.getApplicationsByJobSeeker(seekerId);

        String skills = "";
        Optional<User> userOpt = userService.findUserById(seekerId);
        if (userOpt.isPresent()) {
            skills = userOpt.get().getSkills();
        }

        List<JobRecommendation> recommendations = recommendationService.recommendJobsBySkills(skills);

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);

        model.addAttribute("applicationsCount", myApps.size());
        model.addAttribute("shortlistedCount", myApps.stream()
                .filter(a -> "SHORTLISTED".equalsIgnoreCase(a.getStatus()) || "APPROVED".equalsIgnoreCase(a.getStatus()))
                .count());
        model.addAttribute("unreadCount", notificationDAO != null ? notificationDAO.getUnreadCount(seekerId) : 0);
        model.addAttribute("unreadMessagesCount", messageService != null ? messageService.getUnreadCount(seekerId) : 0);
        model.addAttribute("notifications", notificationDAO != null ? notificationDAO.findByUserId(seekerId) : Collections.emptyList());
        model.addAttribute("recommendations", recommendations.stream().limit(4).toList());
        model.addAttribute("latestJobs", jobService.getAllActiveJobs().stream().limit(5).toList());

        return "jobseeker/dashboard";
    }

    @GetMapping("/jobs")
    public String jobs(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String location,
                       @RequestParam(required = false) String jobType,
                       @RequestParam(required = false) Double minSalary,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String success,
                       @RequestParam(required = false) String error,
                       Model model, HttpSession session) {
        int pageSize = 6;
        List<Job> jobs = jobService.searchJobs(keyword, location, jobType, minSalary, page, pageSize);
        int totalMatches = jobService.countSearchJobs(keyword, location, jobType, minSalary);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalMatches / pageSize));

        int seekerId = getSessionUserId(session);
        Set<Integer> appliedJobIds = new HashSet<>();
        if (seekerId > 0) {
            applicationService.getApplicationsByJobSeeker(seekerId)
                    .forEach(a -> appliedJobIds.add(a.getJobId()));
        }

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);

        model.addAttribute("jobs", jobs);
        model.addAttribute("keyword", keyword);
        model.addAttribute("location", location);
        model.addAttribute("jobType", jobType);
        model.addAttribute("minSalary", minSalary);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalMatches", totalMatches);
        model.addAttribute("appliedJobIds", appliedJobIds);

        return "jobseeker/jobs";
    }

    @GetMapping("/job-details")
    public String jobDetails(@RequestParam(defaultValue = "1") int id,
                             Model model, HttpSession session) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));

        try {
            Job job = jobService.findJobById(id);
            model.addAttribute("job", job);
            int seekerId = getSessionUserId(session);
            boolean alreadyApplied = seekerId > 0 && applicationService.hasUserApplied(id, seekerId);
            model.addAttribute("alreadyApplied", alreadyApplied);
        } catch (Exception e) {
            model.addAttribute("error", "Job not found");
        }

        return "jobseeker/job-details";
    }

    @GetMapping("/applications")
    public String applications(@RequestParam(required = false) String success,
                               @RequestParam(required = false) String error,
                               Model model, HttpSession session) {
        int seekerId = getSessionUserId(session);
        List<Application> apps = applicationService.getApplicationsByJobSeeker(seekerId);

        Map<Integer, List<StatusHistory>> timelineMap = new HashMap<>();
        Map<Integer, Job> jobMap = new HashMap<>();
        for (Application app : apps) {
            timelineMap.put(app.getId(), applicationService.getStatusHistory(app.getId()));
            jobService.getAllJobs().stream()
                    .filter(j -> j.getId() == app.getJobId())
                    .findFirst()
                    .ifPresent(j -> jobMap.put(app.getJobId(), j));
        }

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("applications", apps);
        model.addAttribute("timelineMap", timelineMap);
        model.addAttribute("jobMap", jobMap);

        return "jobseeker/applications";
    }

    @GetMapping("/messages")
    public String messages(@RequestParam(required = false) Integer withUser,
                           @RequestParam(required = false) String success,
                           @RequestParam(required = false) String error,
                           Model model, HttpSession session) {
        int seekerId = getSessionUserId(session);
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);

        // Fetch employer contacts
        List<User> employers = userService.getAllUsers().stream()
                .filter(u -> u instanceof Employer)
                .toList();

        int activeEmployerId = (withUser != null) ? withUser : (!employers.isEmpty() ? employers.get(0).getId() : 2);
        User activeEmployer = userService.findUserById(activeEmployerId).orElse(null);

        List<Message> conversation = messageService != null
                ? messageService.getConversation(seekerId, activeEmployerId)
                : List.of();

        model.addAttribute("employers", employers);
        model.addAttribute("selectedUser", activeEmployer);
        model.addAttribute("conversation", conversation);
        model.addAttribute("currentUserId", seekerId);

        return "jobseeker/messages";
    }

    @GetMapping("/profile")
    public String profile(@RequestParam(required = false) String success,
                          @RequestParam(required = false) String error,
                          Model model, HttpSession session) {
        int seekerId = getSessionUserId(session);
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);

        userService.findUserById(seekerId).ifPresent(u -> {
            model.addAttribute("user", u);
            model.addAttribute("skills", u.getSkills());
            model.addAttribute("resumePath", u.getDefaultResumePath());
        });

        return "jobseeker/profile";
    }

    @GetMapping("/recommendations")
    public String recommendations(@RequestParam(required = false) String skills,
                                  @RequestParam(required = false) String location,
                                  Model model, HttpSession session) {
        int seekerId = getSessionUserId(session);
        String targetSkills = skills;
        if (targetSkills == null || targetSkills.isBlank()) {
            Optional<User> userOpt = userService.findUserById(seekerId);
            targetSkills = userOpt.map(User::getSkills).orElse("Java, Spring Boot, SQL");
        }

        List<JobRecommendation> recommendations = recommendationService.recommendJobsBySkills(targetSkills);

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("skills", targetSkills);
        model.addAttribute("location", location);
        model.addAttribute("recommendations", recommendations);

        return "jobseeker/recommendations";
    }

    private int getSessionUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        return userId instanceof Integer ? (Integer) userId : 0;
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
