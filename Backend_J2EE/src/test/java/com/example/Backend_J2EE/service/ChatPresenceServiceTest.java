package com.example.Backend_J2EE.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPresenceServiceTest {

    private final ChatPresenceService chatPresenceService = new ChatPresenceService();

    @Test
    void markOnlineAndOfflineTracksPresence() {
        chatPresenceService.markAdminOnline(1, "s1");

        assertTrue(chatPresenceService.isAdminOnline(1));
        assertTrue(chatPresenceService.markAdminSessionOffline(1, "s1"));
        assertFalse(chatPresenceService.isAdminOnline(1));
        assertTrue(chatPresenceService.findAdminsOfflineBefore(LocalDateTime.now().plusSeconds(1)).contains(1));
    }

    @Test
    void additionalSessionsKeepAdminOnlineUntilLastSessionLeaves() {
        chatPresenceService.markAdminOnline(2, "s1");
        chatPresenceService.markAdminOnline(2, "s2");

        assertFalse(chatPresenceService.markAdminSessionOffline(2, "s1"));
        assertTrue(chatPresenceService.isAdminOnline(2));
        assertTrue(chatPresenceService.markAdminSessionOffline(2, "s2"));
        assertFalse(chatPresenceService.isAdminOnline(2));
    }

    @Test
    void invalidInputsAreIgnored() {
        chatPresenceService.markAdminOnline(null, "s1");
        chatPresenceService.markAdminOnline(3, " ");

        assertFalse(chatPresenceService.isAdminOnline(3));
        assertFalse(chatPresenceService.markAdminSessionOffline(null, "s1"));
        assertFalse(chatPresenceService.markAdminSessionOffline(3, " "));
        assertTrue(chatPresenceService.findAdminsOfflineBefore(null).isEmpty());
        chatPresenceService.clearOfflineMarker(null);
    }

    @Test
    void duplicateSessionIdsDoNotRequireMultipleOfflineCalls() {
        chatPresenceService.markAdminOnline(4, "same-session");
        chatPresenceService.markAdminOnline(4, "same-session");

        assertTrue(chatPresenceService.isAdminOnline(4));
        assertTrue(chatPresenceService.markAdminSessionOffline(4, "same-session"));
        assertFalse(chatPresenceService.isAdminOnline(4));
    }

    @Test
    void unknownSessionDoesNotMarkOfflineWhenOtherSessionsExist() {
        chatPresenceService.markAdminOnline(5, "s1");
        chatPresenceService.markAdminOnline(5, "s2");

        assertFalse(chatPresenceService.markAdminSessionOffline(5, "unknown"));
        assertTrue(chatPresenceService.isAdminOnline(5));
    }

    @Test
    void markOnlineClearsOfflineMarker() {
        chatPresenceService.markAdminOnline(6, "s1");
        assertTrue(chatPresenceService.markAdminSessionOffline(6, "s1"));
        assertTrue(chatPresenceService.findAdminsOfflineBefore(LocalDateTime.now().plusSeconds(1)).contains(6));

        chatPresenceService.markAdminOnline(6, "s2");

        assertTrue(chatPresenceService.isAdminOnline(6));
        assertTrue(chatPresenceService.findAdminsOfflineBefore(LocalDateTime.now().plusSeconds(1)).isEmpty());
    }

    @Test
    void clearOfflineMarkerRemovesAdminFromOfflineScan() {
        chatPresenceService.markAdminOnline(7, "s1");
        assertTrue(chatPresenceService.markAdminSessionOffline(7, "s1"));
        assertTrue(chatPresenceService.findAdminsOfflineBefore(LocalDateTime.now().plusSeconds(1)).contains(7));

        chatPresenceService.clearOfflineMarker(7);

        List<Integer> offline = chatPresenceService.findAdminsOfflineBefore(LocalDateTime.now().plusSeconds(1));
        assertFalse(offline.contains(7));
    }

    @Test
    void markOfflineReturnsTrueWhenAdminHasNoSessions() {
        assertTrue(chatPresenceService.markAdminSessionOffline(8, "missing-session"));
        assertFalse(chatPresenceService.isAdminOnline(8));
        assertEquals(List.of(), chatPresenceService.findAdminsOfflineBefore(LocalDateTime.now().plusSeconds(1)));
    }
}