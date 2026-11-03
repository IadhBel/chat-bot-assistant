package com.acme.assistant.rag.sync;

import com.acme.assistant.infrastructure.config.AcmeProperties;
import com.acme.assistant.rag.KnowledgeIngestionService;
import com.acme.assistant.rag.KnowledgeIngestionService.IngestResult;
import com.acme.assistant.rag.index.ObjectIndex;
import com.acme.assistant.rag.source.BlobObject;
import com.acme.assistant.rag.source.KnowledgeSource;
import com.acme.assistant.rag.source.KnowledgeSourceConfiguration.KnowledgeSourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Incremental pull from every configured store (classpath, FS, GCS, S3, Azure).
 * One corpus generation is published afterwards so Flash and Pro retrieve the
 * same snapshot — that is the LLM sync contract.
 */
@Service
@ConditionalOnProperty(name = "acme.rag.backend", havingValue = "local", matchIfMissing = true)
public class KnowledgeSyncService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSyncService.class);

    private final KnowledgeSourceRegistry registry;
    private final KnowledgeIngestionService ingestionService;
    private final ObjectIndex objectIndex;
    private final LlmCorpusSynchronizer llmSynchronizer;
    private final boolean ingestOnStartup;
    private final boolean syncEnabled;
    private final boolean deleteMissing;
    private final AtomicReference<SyncReport> lastReport = new AtomicReference<>(
            new SyncReport(0, 0, 0, 0, 0, Instant.EPOCH, "never")
    );

    public KnowledgeSyncService(
            KnowledgeSourceRegistry registry,
            KnowledgeIngestionService ingestionService,
            ObjectIndex objectIndex,
            LlmCorpusSynchronizer llmSynchronizer,
            AcmeProperties properties
    ) {
        this.registry = registry;
        this.ingestionService = ingestionService;
        this.objectIndex = objectIndex;
        this.llmSynchronizer = llmSynchronizer;
        this.ingestOnStartup = properties.rag().ingestOnStartup();
        this.syncEnabled = properties.rag().sync().enabled();
        this.deleteMissing = properties.rag().sync().deleteMissing();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ingestOnStartup) {
            log.info("Skipping startup knowledge sync");
            return;
        }
        sync("startup");
    }

    @Scheduled(fixedDelayString = "${acme.rag.sync.interval:PT15M}")
    public void scheduledSync() {
        if (!syncEnabled) {
            return;
        }
        sync("schedule");
    }

    public SyncReport sync(String trigger) {
        int seen = 0;
        int ingested = 0;
        int images = 0;
        int skipped = 0;
        Set<String> liveUris = new HashSet<>();
        for (KnowledgeSource source : registry.sources()) {
            for (BlobObject object : source.list()) {
                seen++;
                liveUris.add(object.uri());
                var payload = source.load(object.uri());
                if (payload.isEmpty()) {
                    skipped++;
                    continue;
                }
                var result = ingestionService.ingestBlob(payload.get());
                if (result.isEmpty()) {
                    skipped++;
                    continue;
                }
                IngestResult ingest = result.get();
                ingested += ingest.documents();
                images += ingest.images();
            }
        }
        int removed = 0;
        if (deleteMissing) {
            for (String uri : objectIndex.uris()) {
                if (!liveUris.contains(uri) && !uri.startsWith("inline:")) {
                    ingestionService.remove(uri);
                    removed++;
                }
            }
        }
        llmSynchronizer.align(ingestionService.corpusGeneration());
        SyncReport report = new SyncReport(
                seen,
                ingested,
                images,
                skipped,
                removed,
                Instant.now(),
                trigger
        );
        lastReport.set(report);
        log.info(
                "Knowledge sync [{}]: seen={} ingested={} images={} skipped={} removed={} generation={}",
                trigger, seen, ingested, images, skipped, removed, ingestionService.corpusGeneration()
        );
        return report;
    }

    public SyncReport lastReport() {
        return lastReport.get();
    }

    public record SyncReport(
            int seen,
            int ingested,
            int images,
            int skipped,
            int removed,
            Instant at,
            String trigger
    ) {
    }
}
