package com.example.Backend_J2EE.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatPresenceService {

    private final ConcurrentHashMap<Integer, Set<String>> adminSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, LocalDateTime> adminOfflineSince = new ConcurrentHashMap<>();

    public void markAdminOnline(Integer adminId, String sessionId) {
        if (adminId == null || sessionId == null || sessionId.isBlank()) {
            return;
        }
        adminSessions.compute(adminId, (key, oldSet) -> {
            Set<String> set = oldSet == null ? ConcurrentHashMap.newKeySet() : oldSet;
            set.add(sessionId);
            return set;
        });
        adminOfflineSince.remove(adminId);
    }

    public boolean markAdminSessionOffline(Integer adminId, String sessionId) {
        if (adminId == null || sessionId == null || sessionId.isBlank()) {
            return false;
        }
        Set<String> current = adminSessions.get(adminId);
        if (current == null) {
            return true;
        }
        current.remove(sessionId);
        if (current.isEmpty()) {
            adminSessions.remove(adminId);
            adminOfflineSince.put(adminId, LocalDateTime.now());
            return true;
        }
        return false;
    }

    public boolean isAdminOnline(Integer adminId) {
        if (adminId == null) {
            return false;
        }
        Set<String> sessions = adminSessions.get(adminId);
        return sessions != null && !sessions.isEmpty();
    }

    public List<Integer> findAdminsOfflineBefore(LocalDateTime cutoff) {
        if (cutoff == null) {
            return List.of();
        }
        return adminOfflineSince.entrySet()
                .stream()
                .filter(entry -> {
                    Integer adminId = entry.getKey();
                    LocalDateTime offlineSince = entry.getValue();
                    return offlineSince != null && !offlineSince.isAfter(cutoff) && !isAdminOnline(adminId);
                })
                .map(Map.Entry::getKey)
                .toList();
    }

    public void clearOfflineMarker(Integer adminId) {
        if (adminId == null) {
            return;
        }
        adminOfflineSince.remove(adminId);
    }
}
