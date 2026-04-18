package com.example.Backend_J2EE.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPrincipalTest {

    @Test
    void exposesAccountMetadata() {
        ChatPrincipal principal = new ChatPrincipal(42, "alice", "admin");

        assertEquals("42", principal.getName());
        assertEquals(42, principal.getAccountId());
        assertEquals("alice", principal.getUsername());
        assertEquals("admin", principal.getRole());
        assertTrue(principal.isAdmin());
    }

    @Test
    void handlesMissingAccountIdAndNonAdminRole() {
        ChatPrincipal principal = new ChatPrincipal(null, "bob", "user");

        assertEquals("", principal.getName());
        assertFalse(principal.isAdmin());
    }

    @Test
    void detectsAdminRoleCaseInsensitively() {
        ChatPrincipal principal = new ChatPrincipal(7, "carol", "AdMiN");

        assertEquals("7", principal.getName());
        assertTrue(principal.isAdmin());
    }

    @Test
    void treatsNullRoleAsNonAdmin() {
        ChatPrincipal principal = new ChatPrincipal(8, "dave", null);

        assertFalse(principal.isAdmin());
        assertEquals("8", principal.getName());
    }
}