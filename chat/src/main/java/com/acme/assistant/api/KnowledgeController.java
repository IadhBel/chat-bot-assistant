package com.acme.assistant.api;

import com.acme.assistant.api.dto.IngestRequest;
import com.acme.assistant.api.dto.IngestResponse;
import com.acme.assistant.rag.KnowledgeDocument;
import com.acme.assistant.rag.KnowledgeIngestionService;
import com.acme.assistant.rag.sync.KnowledgeSyncService;
import com.acme.assistant.rag.sync.LlmCorpusSynchronizer;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
@ConditionalOnProperty(name = "acme.rag.backend", havingValue = "local", matchIfMissing = true)
public class KnowledgeController {

    private final KnowledgeIngestionService ingestionService;
    private final KnowledgeSyncService syncService;
    private final LlmCorpusSynchronizer llmSynchronizer;

    public KnowledgeController(
            KnowledgeIngestionService ingestionService,
            KnowledgeSyncService syncService,
            LlmCorpusSynchronizer llmSynchronizer
    ) {
        this.ingestionService = ingestionService;
        this.syncService = syncService;
        this.llmSynchronizer = llmSynchronizer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<IngestResponse> ingest(@Valid @RequestBody IngestRequest request) {
        return Mono.fromCallable(() -> {
                    KnowledgeIngestionService.IngestResult result = ingestionService.ingest(
                            List.of(new KnowledgeDocument(request.source(), request.text())));
                    llmSynchronizer.align(result.corpusGeneration());
                    return new IngestResponse(
                            result.documents(),
                            result.segments(),
                            result.images(),
                            result.corpusGeneration()
                    );
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/sync")
    public Mono<KnowledgeSyncService.SyncReport> sync() {
        return Mono.fromCallable(() -> syncService.sync("api"))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/sync")
    public Mono<KnowledgeSyncService.SyncReport> lastSync() {
        return Mono.just(syncService.lastReport());
    }
}
