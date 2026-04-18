package com.example.Backend_J2EE.service.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagIndexMaintenanceServiceTest {

    @Mock
    private RagChatService ragChatService;

    private RagIndexMaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new RagIndexMaintenanceService(ragChatService, true, true);
    }

    @Test
    void startupRebuildRunsOnceUntilDirtyAgain() {
        when(ragChatService.rebuildIndex()).thenReturn(3);

        service.onApplicationReady();
        service.scheduledRebuild();
        service.markDirty();
        service.scheduledRebuild();

        verify(ragChatService, times(2)).rebuildIndex();
    }

    @Test
    void startupRebuildCanBeDisabled() {
        RagIndexMaintenanceService disabled = new RagIndexMaintenanceService(ragChatService, false, true);
        disabled.onApplicationReady();

        verify(ragChatService, never()).rebuildIndex();
    }

    @Test
    void scheduledRebuildCanBeDisabled() {
        RagIndexMaintenanceService disabled = new RagIndexMaintenanceService(ragChatService, true, false);
        disabled.scheduledRebuild();

        verify(ragChatService, never()).rebuildIndex();
    }

    @Test
    void scheduledRebuildSkipsWhenAlreadyRunning() throws Exception {
        setAtomicBoolean(service, "running", true);
        service.scheduledRebuild();

        verify(ragChatService, never()).rebuildIndex();
    }

    @Test
    void rebuildFailureCanRecoverOnNextRun() {
        when(ragChatService.rebuildIndex()).thenThrow(new RuntimeException("boom")).thenReturn(4);

        service.onApplicationReady();
        service.markDirty();
        service.scheduledRebuild();

        verify(ragChatService, times(2)).rebuildIndex();
    }

    private void setAtomicBoolean(RagIndexMaintenanceService target, String fieldName, boolean value) throws Exception {
        Field field = RagIndexMaintenanceService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        AtomicBoolean flag = (AtomicBoolean) field.get(target);
        flag.set(value);
    }
}