package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.account.AccountProfileResponse;
import com.example.Backend_J2EE.dto.auth.AuthMessageResponse;
import com.example.Backend_J2EE.dto.auth.ForgotPasswordRequest;
import com.example.Backend_J2EE.dto.auth.GoogleLoginRequest;
import com.example.Backend_J2EE.dto.auth.LoginRequest;
import com.example.Backend_J2EE.dto.auth.RegisterRequest;
import com.example.Backend_J2EE.dto.auth.ResetPasswordRequest;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.service.AuthService;
import com.example.Backend_J2EE.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private CartService cartService;

    @Mock
    private HttpSession session;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, cartService);
    }

    @Test
    void loginStoresSessionAndMergesCart() {
        Account account = Account.builder().id(21).username("user").build();
        when(authService.login(any(LoginRequest.class))).thenReturn(account);
        when(authService.toProfile(account)).thenReturn(new AccountProfileResponse(21, "user", "user@example.com", null, "user", null));

        AccountProfileResponse response = controller.login(new LoginRequest("user", "pw"), session);

        assertEquals(21, response.getId());
        verify(session).setAttribute(AuthService.SESSION_ACCOUNT_ID, 21);
        verify(cartService).mergeSessionCartToDatabase(21, session);
    }

    @Test
    void registerDelegatesToAuthService() {
        when(authService.register(any(RegisterRequest.class))).thenReturn(new AccountProfileResponse(22, "new", "new@example.com", null, "user", null));

        AccountProfileResponse response = controller.register(new RegisterRequest("new", "pw", "new@example.com", null));

        assertEquals(22, response.getId());
    }

    @Test
    void forgotAndResetPasswordDelegate() {
        when(authService.requestPasswordReset(any(ForgotPasswordRequest.class))).thenReturn(new AuthMessageResponse("sent"));
        when(authService.resetPassword(any(ResetPasswordRequest.class))).thenReturn(new AuthMessageResponse("ok"));

        assertEquals("sent", controller.forgotPassword(new ForgotPasswordRequest("a@b.com")).getMessage());
        assertEquals("ok", controller.resetPassword(new ResetPasswordRequest("t", "p")).getMessage());
    }

    @Test
    void logoutInvalidatesSession() {
        controller.logout(session);
        verify(session).invalidate();
    }

    @Test
    void googleLoginDelegates() {
        Account account = Account.builder().id(23).username("google").build();
        when(authService.loginWithGoogle(any(GoogleLoginRequest.class))).thenReturn(account);
        when(authService.toProfile(account)).thenReturn(new AccountProfileResponse(23, "google", "g@example.com", null, "user", null));

        AccountProfileResponse response = controller.loginWithGoogle(new GoogleLoginRequest(), session);

        assertEquals(23, response.getId());
        verify(cartService).mergeSessionCartToDatabase(23, session);
    }
}