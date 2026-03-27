package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminUserResponse {
    private Integer id;
    private String username;
    private String email;
    private String phone;
    private String role;
    private Boolean locked;
    private LocalDateTime createdAt;
}
