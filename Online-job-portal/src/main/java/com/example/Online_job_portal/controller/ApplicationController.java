package com.example.Online_job_portal.controller;

import com.example.Online_job_portal.exception.ApplicationException;
import com.example.Online_job_portal.model.Application;
import com.example.Online_job_portal.service.ApplicationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public List<Application> getApplications() {
        return applicationService.getAllApplications();
    }
}
