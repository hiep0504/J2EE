package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.account.AccountProfileResponse;
import com.example.Backend_J2EE.dto.account.UpdateProfileRequest;
import com.example.Backend_J2EE.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AccountController accountController;

    @Test
    void meDelegatesToAuthService() {
        AccountProfileResponse profile = new AccountProfileResponse();
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(5);
        when(authService.getProfile(5)).thenReturn(profile);

        AccountProfileResponse result = accountController.me(session);

        assertSame(profile, result);
        verify(authService).getProfile(5);
    }

    @Test
    void updateMeDelegatesToAuthService() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        AccountProfileResponse profile = new AccountProfileResponse();
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(5);
        when(authService.updateProfile(5, request)).thenReturn(profile);

        AccountProfileResponse result = accountController.updateMe(request, session);

        assertSame(profile, result);
        verify(authService).updateProfile(5, request);
    }

    @Test
    void meAndUpdateMePassNullAccountIdWhenSessionMissing() {
        AccountProfileResponse profile = new AccountProfileResponse();
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(null);
        when(authService.getProfile(null)).thenReturn(profile);
        when(authService.updateProfile(null, request)).thenReturn(profile);

        assertSame(profile, accountController.me(session));
        assertSame(profile, accountController.updateMe(request, session));
        verify(authService).getProfile(null);
        verify(authService).updateProfile(null, request);
    }
}
