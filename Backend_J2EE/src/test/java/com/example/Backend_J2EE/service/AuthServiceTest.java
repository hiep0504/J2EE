package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.account.AccountProfileResponse;
import com.example.Backend_J2EE.dto.account.UpdateProfileRequest;
import com.example.Backend_J2EE.dto.auth.AuthMessageResponse;
import com.example.Backend_J2EE.dto.auth.ForgotPasswordRequest;
import com.example.Backend_J2EE.dto.auth.LoginRequest;
import com.example.Backend_J2EE.dto.auth.RegisterRequest;
import com.example.Backend_J2EE.dto.auth.ResetPasswordRequest;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.repository.AccountRepository;
import com.example.Backend_J2EE.util.PasswordHasher;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JavaMailSender mailSender;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(accountRepository, mailSender, "google-client", "http://frontend", "noreply@example.com");
    }

    @Test
    void registerCreatesAccountProfile() {
        RegisterRequest request = new RegisterRequest("newuser", "secret123", "user@example.com", "0123");
        when(accountRepository.existsByUsername("newuser")).thenReturn(false);
        when(accountRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(10);
            account.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
            return account;
        });

        AccountProfileResponse response = authService.register(request);

        assertEquals(10, response.getId());
        assertEquals("newuser", response.getUsername());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("0123", response.getPhone());
        assertEquals("user", response.getRole());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertTrue(PasswordHasher.matches("secret123", captor.getValue().getPassword()));
        assertFalse("secret123".equals(captor.getValue().getPassword()));
    }

    @Test
    void registerRejectsMissingRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("newuser", "secret123", "user@example.com", "0123");
        when(accountRepository.existsByUsername("newuser")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("newuser", "secret123", "user@example.com", "0123");
        when(accountRepository.existsByUsername("newuser")).thenReturn(false);
        when(accountRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void loginUpgradesLegacyPasswordHash() {
        Account account = Account.builder()
                .id(11)
                .username("legacy")
                .email("legacy@example.com")
                .password("plain-password")
                .role(Account.Role.user)
                .loginType(Account.LoginType.local)
                .locked(false)
                .build();

        when(accountRepository.findByUsernameOrEmail("legacy", "legacy")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account loggedIn = authService.login(new LoginRequest("legacy", "plain-password"));

        assertEquals(11, loggedIn.getId());
        assertTrue(PasswordHasher.matches("plain-password", loggedIn.getPassword()));
        verify(accountRepository).save(account);
    }

        @Test
        void loginRejectsWrongPassword() {
        Account account = Account.builder()
            .id(11)
            .username("legacy")
            .email("legacy@example.com")
            .password(PasswordHasher.hash("correct-pass"))
            .role(Account.Role.user)
            .loginType(Account.LoginType.local)
            .locked(false)
            .build();

        when(accountRepository.findByUsernameOrEmail("legacy", "legacy")).thenReturn(Optional.of(account));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> authService.login(new LoginRequest("legacy", "wrong-pass")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        void loginUsesEmailFallbackLookup() {
        Account account = Account.builder()
            .id(20)
            .username("emailuser")
            .email("email@example.com")
            .password(PasswordHasher.hash("secret"))
            .loginType(Account.LoginType.local)
            .locked(false)
            .build();

        when(accountRepository.findByUsernameOrEmail("email@example.com", "email@example.com")).thenReturn(Optional.empty());
        when(accountRepository.findByEmailIgnoreCase("email@example.com")).thenReturn(Optional.of(account));

        Account loggedIn = authService.login(new LoginRequest("email@example.com", "secret"));

        assertEquals(20, loggedIn.getId());
        }

        @Test
        void loginRejectsGoogleAccountForPasswordLogin() {
        Account account = Account.builder()
            .id(21)
            .username("gg")
            .email("gg@example.com")
            .password(null)
            .loginType(Account.LoginType.google)
            .locked(false)
            .build();
        when(accountRepository.findByUsernameOrEmail("gg", "gg")).thenReturn(Optional.of(account));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> authService.login(new LoginRequest("gg", "any")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        void loginRejectsLockedAccount() {
        Account account = Account.builder()
            .id(22)
            .username("locked")
            .email("locked@example.com")
            .password(PasswordHasher.hash("secret"))
            .loginType(Account.LoginType.local)
            .locked(true)
            .build();
        when(accountRepository.findByUsernameOrEmail("locked", "locked")).thenReturn(Optional.of(account));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> authService.login(new LoginRequest("locked", "secret")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        void getAccountOrThrowRejectsNullId() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> authService.getAccountOrThrow(null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        void getAccountOrThrowRejectsMissingAccount() {
        when(accountRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> authService.getAccountOrThrow(999));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

    @Test
    void getProfileReturnsMappedResponse() {
        Account account = Account.builder()
                .id(12)
                .username("viewer")
                .email("viewer@example.com")
                .phone("0909")
                .role(Account.Role.user)
                .createdAt(LocalDateTime.of(2024, 2, 2, 9, 0))
                .build();

        when(accountRepository.findById(12)).thenReturn(Optional.of(account));

        AccountProfileResponse response = authService.getProfile(12);

        assertEquals(12, response.getId());
        assertEquals("viewer", response.getUsername());
        assertEquals("viewer@example.com", response.getEmail());
        assertEquals("0909", response.getPhone());
    }

    @Test
    void updateProfileRejectsDuplicateEmail() {
        Account account = Account.builder().id(13).email("old@example.com").build();
        when(accountRepository.findById(13)).thenReturn(Optional.of(account));
        when(accountRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest("new@example.com", "  ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.updateProfile(13, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void updateProfileRejectsNullRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.updateProfile(13, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateProfileRejectsBlankEmail() {
        Account account = Account.builder().id(13).email("old@example.com").build();
        when(accountRepository.findById(13)).thenReturn(Optional.of(account));

        UpdateProfileRequest request = new UpdateProfileRequest("   ", "0909");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.updateProfile(13, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateProfileMapsBlankPhoneToNull() {
        Account account = Account.builder().id(13).email("old@example.com").phone("old").build();
        when(accountRepository.findById(13)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("new@example.com", "   ");
        AccountProfileResponse response = authService.updateProfile(13, request);

        assertEquals("new@example.com", response.getEmail());
        assertNull(response.getPhone());
    }

    @Test
    void requestPasswordResetRequiresMailConfiguration() {
        AuthService noMailService = new AuthService(accountRepository, mailSender, "google-client", "http://frontend", "");
        when(accountRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(Account.builder().id(14).username("user").email("user@example.com").build()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> noMailService.requestPasswordReset(new ForgotPasswordRequest("user@example.com")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void requestPasswordResetIgnoresUnknownEmail() {
        when(accountRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        AuthMessageResponse response = authService.requestPasswordReset(new ForgotPasswordRequest("missing@example.com"));

        assertNotNull(response.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void requestPasswordResetSavesTokenAndSendsEmail() {
        Account account = Account.builder().id(14).username("user").email("user@example.com").build();
        when(accountRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        AuthMessageResponse response = authService.requestPasswordReset(new ForgotPasswordRequest("user@example.com"));

        assertNotNull(response.getMessage());
        assertNotNull(account.getPasswordResetTokenHash());
        assertNotNull(account.getPasswordResetTokenExpiresAt());
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void resetPasswordUpdatesAccountAndClearsToken() {
        Account account = Account.builder()
                .id(15)
                .username("resetme")
                .email("reset@example.com")
                .passwordResetTokenHash("hash")
                .passwordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(1))
                .build();

        when(accountRepository.findByPasswordResetTokenHashAndPasswordResetTokenExpiresAtAfter(anyString(), any())).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthMessageResponse response = authService.resetPassword(new ResetPasswordRequest("token", "newpass"));

        assertNotNull(response.getMessage());
        assertTrue(PasswordHasher.matches("newpass", account.getPassword()));
        assertEquals(Account.LoginType.local, account.getLoginType());
        assertEquals(null, account.getPasswordResetTokenHash());
        assertEquals(null, account.getPasswordResetTokenExpiresAt());
    }

    @Test
    void resetPasswordRejectsNullRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void resetPasswordRejectsMissingToken() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("   ", "newpass")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void resetPasswordRejectsInvalidOrExpiredToken() {
        when(accountRepository.findByPasswordResetTokenHashAndPasswordResetTokenExpiresAtAfter(anyString(), any()))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("token", "newpass")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}