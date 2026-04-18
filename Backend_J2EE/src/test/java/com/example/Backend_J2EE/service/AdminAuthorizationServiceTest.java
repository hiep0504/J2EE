package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.entity.Account;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AdminAuthorizationService adminAuthorizationService;

    @Test
    void requireAdmin_returnsAdminAccount() {
        Account admin = Account.builder().id(10).role(Account.Role.admin).locked(false).build();
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(10);
        when(authService.getAccountOrThrow(10)).thenReturn(admin);

        Account result = adminAuthorizationService.requireAdmin(session);

        assertEquals(admin, result);
    }

    @Test
    void requireAdminRejectsNonAdmin() {
        Account user = Account.builder().id(11).role(Account.Role.user).locked(false).build();
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(11);
        when(authService.getAccountOrThrow(11)).thenReturn(user);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> adminAuthorizationService.requireAdmin(session));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void requireAdminRejectsLockedAdmin() {
        Account admin = Account.builder().id(12).role(Account.Role.admin).locked(true).build();
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(12);
        when(authService.getAccountOrThrow(12)).thenReturn(admin);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> adminAuthorizationService.requireAdmin(session));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void requireAdminRejectsWhenSessionHasNoAccountId() {
        ResponseStatusException unauthorized = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap");
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(null);
        when(authService.getAccountOrThrow(null)).thenThrow(unauthorized);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminAuthorizationService.requireAdmin(session));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(authService).getAccountOrThrow(null);
    }

    @Test
    void requireAdminPropagatesAuthErrorWhenAccountNotFound() {
        ResponseStatusException unauthorized = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tai khoan khong ton tai");
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(99);
        when(authService.getAccountOrThrow(99)).thenThrow(unauthorized);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminAuthorizationService.requireAdmin(session));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void requireAdminRejectsWhenRoleIsNull() {
        Account account = Account.builder().id(13).role(null).locked(false).build();
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(13);
        when(authService.getAccountOrThrow(13)).thenReturn(account);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminAuthorizationService.requireAdmin(session));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void requireAdminAllowsWhenLockedIsNull() {
        Account admin = Account.builder().id(14).role(Account.Role.admin).locked(null).build();
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(14);
        when(authService.getAccountOrThrow(14)).thenReturn(admin);

        Account result = adminAuthorizationService.requireAdmin(session);

        assertSame(admin, result);
    }
}