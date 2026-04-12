package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    Optional<Account> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Account> findByEmail(String email);

    Optional<Account> findByEmailIgnoreCase(String email);

    Optional<Account> findByGoogleId(String googleId);

    Optional<Account> findByPasswordResetTokenHashAndPasswordResetTokenExpiresAtAfter(String passwordResetTokenHash, java.time.LocalDateTime passwordResetTokenExpiresAt);

    Optional<Account> findByUsernameOrEmail(String username, String email);
}
