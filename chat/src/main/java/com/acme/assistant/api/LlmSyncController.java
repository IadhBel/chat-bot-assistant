package com.acme.assistant.api;

import com.acme.assistant.corpus.VertexRagPort;
import com.acme.assistant.infrastructure.config.AcmeProperties;
import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Backend;
import com.acme.assistant.rag.KnowledgeIngestionService;
import com.acme.assistant.rag.sync.LlmCorpusSynchronizer;
import com.acme.assistant.rag.sync.LlmCorpusSynchronizer.LlmSyncStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/llm")
public class LlmSyncController {

    private final LlmCorpusSynchronizer llmSynchronizer;
    private final ObjectProvider<KnowledgeIngestionService> ingestionService;
    private final VertexRagPort vertexRag;
    private final Backend backend;

    public LlmSyncController(
            LlmCorpusSynchronizer llmSynchronizer,
            ObjectProvider<KnowledgeIngestionService> ingestionService,
            VertexRagPort vertexRag,
            AcmeProperties properties
    ) {
        this.llmSynchronizer = llmSynchronizer;
        this.ingestionService = ingestionService;
        this.vertexRag = vertexRag;
        this.backend = properties.rag().backend();
    }

    @PostMapping("/sync")
    public Mono<LlmSyncStatus> sync() {
        return Mono.fromCallable(() -> llmSynchronizer.align(corpusGeneration()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/sync")
    public Mono<LlmSyncStatus> status() {
        return Mono.just(llmSynchronizer.status());
    }

    private long corpusGeneration() {
        if (backend == Backend.vertex) {
            return vertexRag.corpusGeneration();
        }
        KnowledgeIngestionService ingestion = ingestionService.getIfAvailable();
        return ingestion == null ? 0L : ingestion.corpusGeneration();
    }
}
