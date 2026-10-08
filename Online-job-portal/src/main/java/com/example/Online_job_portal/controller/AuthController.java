package com.example.Online_job_portal.controller;

import com.example.Online_job_portal.model.User;
import com.example.Online_job_portal.service.UserService;
import com.example.Online_job_portal.util.ActivityLogger;
import com.example.Online_job_portal.util.ValidationUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;
    private final ActivityLogger activityLogger;

    public AuthController(UserService userService, ActivityLogger activityLogger) {
        this.userService = userService;
        this.activityLogger = activityLogger;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        return "index";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String registered,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String success,
                        Model model) {
        model.addAttribute("error", error);
        model.addAttribute("registered", registered != null);
        model.addAttribute("logout", logout != null);
        model.addAttribute("success", success);
        return "login";
    }

    @GetMapping("/register")
    public String register(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("error", error);
        return "register";
    }

    @GetMapping("/access-denied")
    public String accessDenied(HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("role", session.getAttribute("role"));
        return "access-denied";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        return "redirect:/servlet/logout";
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
