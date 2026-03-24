package com.example.Backend_J2EE.dto.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountProfileResponse {

    private Integer id;
    private String username;
    private String email;
    private String phone;
    private String role;
    private LocalDateTime createdAt;
}
