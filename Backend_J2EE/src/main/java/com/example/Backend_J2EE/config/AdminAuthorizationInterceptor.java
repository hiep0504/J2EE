package com.example.Backend_J2EE.config;

import com.example.Backend_J2EE.service.AdminAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthorizationInterceptor implements HandlerInterceptor {

    private final AdminAuthorizationService adminAuthorizationService;

    public AdminAuthorizationInterceptor(AdminAuthorizationService adminAuthorizationService) {
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            request.getSession(true);
        }
        adminAuthorizationService.requireAdmin(request.getSession());
        return true;
    }
}
