package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.account.AccountProfileResponse;
import com.example.Backend_J2EE.dto.auth.LoginRequest;
import com.example.Backend_J2EE.dto.auth.RegisterRequest;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountProfileResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AccountProfileResponse login(@RequestBody LoginRequest request, HttpSession session) {
        Account account = authService.login(request);
        session.setAttribute(AuthService.SESSION_ACCOUNT_ID, account.getId());
        return authService.toProfile(account);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        session.invalidate();
    }
}
