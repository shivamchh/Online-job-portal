package com.example.Online_job_portal.config;

import com.example.Online_job_portal.service.LoginAttemptService;
import com.example.Online_job_portal.service.UserService;
import com.example.Online_job_portal.servlet.LoginServlet;
import com.example.Online_job_portal.servlet.RegisterServlet;
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
    public ServletRegistrationBean<RegisterServlet> registerServletRegistration(UserService userService, ActivityLogger activityLogger) {
        RegisterServlet registerServlet = new RegisterServlet(userService, activityLogger);
        ServletRegistrationBean<RegisterServlet> registrationBean = new ServletRegistrationBean<>(registerServlet, "/servlet/register");
        registrationBean.setName("RegisterServlet");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }
}
