package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.account.AccountProfileResponse;
import com.example.Backend_J2EE.dto.account.UpdateProfileRequest;
import com.example.Backend_J2EE.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public AccountProfileResponse me(HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return authService.getProfile(accountId);
    }

    @PutMapping("/me")
    public AccountProfileResponse updateMe(@RequestBody UpdateProfileRequest request, HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return authService.updateProfile(accountId, request);
    }
}
