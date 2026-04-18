package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.entity.Account;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthorizationService {

    private final AuthService authService;

    public AdminAuthorizationService(AuthService authService) {
        this.authService = authService;
    }

    public Account requireAdmin(HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        Account account = authService.getAccountOrThrow(accountId);

        if (account.getRole() != Account.Role.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen truy cap");
        }
        if (Boolean.TRUE.equals(account.getLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan admin da bi khoa");
        }

        return account;
    }
}
