package com.example.Online_job_portal.controller;

import com.example.Online_job_portal.service.AdminService;
import com.example.Online_job_portal.util.ActivityLogger;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final ActivityLogger activityLogger;

    public AdminController(AdminService adminService, ActivityLogger activityLogger) {
        this.adminService = adminService;
        this.activityLogger = activityLogger;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String success,
                            @RequestParam(required = false) String error,
                            HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);

        model.addAttribute("totalUsers", adminService.countUsers());
        model.addAttribute("totalJobs", adminService.countJobs());
        model.addAttribute("activeJobs", adminService.countActiveJobs());
        model.addAttribute("totalApplications", adminService.countApplications());
        model.addAttribute("recentActivities", adminService.getRecentActivities(10));
        model.addAttribute("users", adminService.viewUsers());
        model.addAttribute("jobs", adminService.viewJobs());

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String success,
                        @RequestParam(required = false) String error,
                        HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("users", adminService.viewUsers());
        return "admin/users";
    }

    @GetMapping("/jobs")
    public String jobs(@RequestParam(required = false) String success,
                       @RequestParam(required = false) String error,
                       HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("jobs", adminService.viewJobs());
        return "admin/jobs";
    }

    @GetMapping("/settings")
    public String settings(@RequestParam(required = false) String success,
                           @RequestParam(required = false) String error,
                           HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("settings", adminService.getAllSettings());
        model.addAttribute("settingsList", adminService.findAllSettings());
        model.addAttribute("queueSize", activityLogger != null ? activityLogger.getQueueSize() : 0);
        model.addAttribute("totalUsers", adminService.countUsers());
        model.addAttribute("totalJobs", adminService.countJobs());
        return "admin/settings";
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
