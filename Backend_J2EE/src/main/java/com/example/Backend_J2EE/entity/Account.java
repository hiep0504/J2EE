package com.example.Backend_J2EE.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", unique = true, length = 100)
    private String username;

    @JsonIgnore
    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 10)
    @Builder.Default
    private Role role = Role.user;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", length = 10, columnDefinition = "ENUM('local','google')")
    @Builder.Default
    private LoginType loginType = LoginType.local;

    @Column(name = "google_id", unique = true, length = 100)
    private String googleId;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "is_locked")
    @Builder.Default
    private Boolean locked = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
    private Cart cart;

    public enum Role {
        admin, user
    }

    public enum LoginType {
        local, google
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

