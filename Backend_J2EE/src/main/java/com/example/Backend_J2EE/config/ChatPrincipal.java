package com.example.Backend_J2EE.config;

import java.security.Principal;

public class ChatPrincipal implements Principal {

    private final Integer accountId;
    private final String username;
    private final String role;

    public ChatPrincipal(Integer accountId, String username, String role) {
        this.accountId = accountId;
        this.username = username;
        this.role = role;
    }

    @Override
    public String getName() {
        return accountId != null ? String.valueOf(accountId) : "";
    }

    public Integer getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}
