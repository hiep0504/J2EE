package com.example.Backend_J2EE.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RagIndexMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(RagIndexMaintenanceService.class);

    private final RagChatService ragChatService;
    private final boolean rebuildOnStartup;
    private final boolean scheduledRebuildEnabled;
    private final AtomicBoolean dirty = new AtomicBoolean(true);
    private final AtomicBoolean running = new AtomicBoolean(false);

    public RagIndexMaintenanceService(
            RagChatService ragChatService,
            @Value("${app.rag.index.rebuild-on-startup:true}") boolean rebuildOnStartup,
            @Value("${app.rag.index.scheduled-enabled:true}") boolean scheduledRebuildEnabled
    ) {
        this.ragChatService = ragChatService;
        this.rebuildOnStartup = rebuildOnStartup;
        this.scheduledRebuildEnabled = scheduledRebuildEnabled;
    }

    public void markDirty() {
        dirty.set(true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!rebuildOnStartup) {
            log.info("RAG startup rebuild is disabled");
            return;
        }
        rebuildIfNeeded("startup", true);
    }

    @Scheduled(fixedDelayString = "${app.rag.index.rebuild-interval-ms:1800000}")
    public void scheduledRebuild() {
        if (!scheduledRebuildEnabled) {
            return;
        }
        rebuildIfNeeded("schedule", false);
    }

    private void rebuildIfNeeded(String source, boolean force) {
        if (!force && !dirty.get()) {
            return;
        }

        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            int indexed = ragChatService.rebuildIndex();
            dirty.set(false);
            log.info("RAG index rebuild succeeded from {}. Indexed vectors: {}", source, indexed);
        } catch (Exception ex) {
            log.warn("RAG index rebuild failed from {}: {}", source, ex.getMessage(), ex);
        } finally {
            running.set(false);
        }
    }
}
