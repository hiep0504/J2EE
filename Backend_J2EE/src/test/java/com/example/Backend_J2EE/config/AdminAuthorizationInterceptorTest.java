package com.example.Backend_J2EE.config;

import com.example.Backend_J2EE.service.AdminAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationInterceptorTest {

    @Mock
    private AdminAuthorizationService adminAuthorizationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AdminAuthorizationInterceptor interceptor;

    @Test
    void preHandleSkipsOptionsRequests() {
        when(request.getMethod()).thenReturn("OPTIONS");

        assertTrue(interceptor.preHandle(request, response, new Object()));

        verifyNoInteractions(adminAuthorizationService);
    }

    @Test
    void preHandleCreatesSessionAndChecksAdmin() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession()).thenReturn(session);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        verify(request).getSession(false);
        verify(request).getSession(true);
        verify(adminAuthorizationService).requireAdmin(session);
    }

    @Test
    void preHandleUsesExistingSessionWithoutCreatingNewOne() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        verify(request).getSession(false);
        verify(request).getSession();
        verify(adminAuthorizationService).requireAdmin(session);
    }
}