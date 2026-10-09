package com.example.Online_job_portal.config;

import com.example.Online_job_portal.service.LoginAttemptService;
import com.example.Online_job_portal.service.UserService;
import com.example.Online_job_portal.servlet.*;
import com.example.Online_job_portal.util.ActivityLogger;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class that registers core action servlets via ServletRegistrationBean.
 * Guarantees direct dependency injection and eliminates @WebServlet lifecycle race conditions.
 */
@Configuration
public class ServletConfig {

    @Bean
    public ServletRegistrationBean<LoginServlet> loginServletRegistration(UserService userService, ActivityLogger activityLogger, LoginAttemptService loginAttemptService) {
        LoginServlet loginServlet = new LoginServlet(userService, activityLogger, loginAttemptService);
        ServletRegistrationBean<LoginServlet> registrationBean = new ServletRegistrationBean<>(loginServlet, "/servlet/login");
        registrationBean.setName("LoginServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<RegisterServlet> registerServletRegistration(UserService userService, ActivityLogger activityLogger, com.example.Online_job_portal.dao.SettingsDAO settingsDAO) {
        RegisterServlet registerServlet = new RegisterServlet(userService, activityLogger, settingsDAO);
        ServletRegistrationBean<RegisterServlet> registrationBean = new ServletRegistrationBean<>(registerServlet, "/servlet/register");
        registrationBean.setName("RegisterServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<JobServlet> jobServletRegistration() {
        JobServlet jobServlet = new JobServlet();
        ServletRegistrationBean<JobServlet> registrationBean = new ServletRegistrationBean<>(jobServlet, "/servlet/jobs");
        registrationBean.setName("JobServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<NotificationServlet> notificationServletRegistration() {
        NotificationServlet notificationServlet = new NotificationServlet();
        ServletRegistrationBean<NotificationServlet> registrationBean = new ServletRegistrationBean<>(notificationServlet, "/servlet/notifications");
        registrationBean.setName("NotificationServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<ApplicationServlet> applicationServletRegistration() {
        ApplicationServlet applicationServlet = new ApplicationServlet();
        ServletRegistrationBean<ApplicationServlet> registrationBean = new ServletRegistrationBean<>(applicationServlet, "/servlet/applications");
        registrationBean.setName("ApplicationServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<LogoutServlet> logoutServletRegistration() {
        LogoutServlet logoutServlet = new LogoutServlet();
        ServletRegistrationBean<LogoutServlet> registrationBean = new ServletRegistrationBean<>(logoutServlet, "/servlet/logout");
        registrationBean.setName("LogoutServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<ResumeDownloadServlet> resumeDownloadServletRegistration() {
        ResumeDownloadServlet servlet = new ResumeDownloadServlet();
        ServletRegistrationBean<ResumeDownloadServlet> registrationBean = new ServletRegistrationBean<>(servlet, "/servlet/resume-download");
        registrationBean.setName("ResumeDownloadServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<ResumeUploadServlet> resumeUploadServletRegistration() {
        ResumeUploadServlet servlet = new ResumeUploadServlet();
        ServletRegistrationBean<ResumeUploadServlet> registrationBean = new ServletRegistrationBean<>(servlet, "/servlet/resume-upload");
        registrationBean.setName("ResumeUploadServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<MessageServlet> messageServletRegistration() {
        MessageServlet servlet = new MessageServlet();
        ServletRegistrationBean<MessageServlet> registrationBean = new ServletRegistrationBean<>(servlet, "/servlet/messages");
        registrationBean.setName("MessageServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<ProfileServlet> profileServletRegistration() {
        ProfileServlet servlet = new ProfileServlet();
        ServletRegistrationBean<ProfileServlet> registrationBean = new ServletRegistrationBean<>(servlet, "/servlet/profile");
        registrationBean.setName("ProfileServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<StatisticsServlet> statisticsServletRegistration(
            com.example.Online_job_portal.service.JobService jobService,
            com.example.Online_job_portal.service.ApplicationService applicationService,
            UserService userService) {
        StatisticsServlet servlet = new StatisticsServlet(jobService, applicationService, userService);
        ServletRegistrationBean<StatisticsServlet> registrationBean = new ServletRegistrationBean<>(servlet, "/servlet/statistics", "/api/statistics");
        registrationBean.setName("StatisticsServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<ActivityLogServlet> activityLogServletRegistration(com.example.Online_job_portal.dao.ActivityLogDAO activityLogDAO) {
        ActivityLogServlet servlet = new ActivityLogServlet(activityLogDAO);
        ServletRegistrationBean<ActivityLogServlet> registrationBean = new ServletRegistrationBean<>(servlet, "/servlet/activities", "/api/activities");
        registrationBean.setName("ActivityLogServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<com.example.Online_job_portal.filter.RequestLoggingFilter> requestLoggingFilterRegistration() {
        org.springframework.boot.web.servlet.FilterRegistrationBean<com.example.Online_job_portal.filter.RequestLoggingFilter> registration =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(new com.example.Online_job_portal.filter.RequestLoggingFilter());
        registration.addUrlPatterns("/*");
        registration.setName("RequestLoggingFilter");
        registration.setOrder(0);
        return registration;
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<com.example.Online_job_portal.filter.SecurityHeadersFilter> securityHeadersFilterRegistration() {
        org.springframework.boot.web.servlet.FilterRegistrationBean<com.example.Online_job_portal.filter.SecurityHeadersFilter> registration =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(new com.example.Online_job_portal.filter.SecurityHeadersFilter());
        registration.addUrlPatterns("/*");
        registration.setName("SecurityHeadersFilter");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<com.example.Online_job_portal.filter.AuthFilter> authFilterRegistration() {
        org.springframework.boot.web.servlet.FilterRegistrationBean<com.example.Online_job_portal.filter.AuthFilter> registration =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(new com.example.Online_job_portal.filter.AuthFilter());
        registration.addUrlPatterns("/*");
        registration.setName("AuthFilter");
        registration.setOrder(2);
        return registration;
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<com.example.Online_job_portal.filter.CsrfFilter> csrfFilterRegistration() {
        org.springframework.boot.web.servlet.FilterRegistrationBean<com.example.Online_job_portal.filter.CsrfFilter> registration =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(new com.example.Online_job_portal.filter.CsrfFilter());
        registration.addUrlPatterns("/*");
        registration.setName("CsrfFilter");
        registration.setOrder(3);
        return registration;
    }

    @Bean
    public org.springframework.boot.web.servlet.ServletListenerRegistrationBean<com.example.Online_job_portal.listener.ActiveSessionListener> activeSessionListenerRegistration() {
        return new org.springframework.boot.web.servlet.ServletListenerRegistrationBean<>(new com.example.Online_job_portal.listener.ActiveSessionListener());
    }

    @Bean
    public org.springframework.boot.web.servlet.ServletListenerRegistrationBean<com.example.Online_job_portal.listener.AppContextListener> appContextListenerRegistration() {
        return new org.springframework.boot.web.servlet.ServletListenerRegistrationBean<>(new com.example.Online_job_portal.listener.AppContextListener());
    }
}
