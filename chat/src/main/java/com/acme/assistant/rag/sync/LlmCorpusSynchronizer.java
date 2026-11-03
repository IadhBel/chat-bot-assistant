package com.acme.assistant.rag.sync;

import com.acme.assistant.infrastructure.config.AcmeProperties;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * After a corpus change, both Gemini SKUs are pinged so they share the same
 * generation number. Retrieval is already shared (one EmbeddingStore); this
 * step proves the chat endpoints are live on that snapshot.
 */
@Service
public class LlmCorpusSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(LlmCorpusSynchronizer.class);

    private final ChatModel flash;
    private final ChatModel pro;
    private final String flashName;
    private final String proName;
    private final AtomicReference<LlmSyncStatus> lastStatus = new AtomicReference<>(
            LlmSyncStatus.unknown()
    );

    public LlmCorpusSynchronizer(
            @Qualifier("geminiFlashChatModel") ChatModel flash,
            @Qualifier("geminiProChatModel") ChatModel pro,
            AcmeProperties properties
    ) {
        this.flash = flash;
        this.pro = pro;
        this.flashName = properties.llm().vertex().flashModel();
        this.proName = properties.llm().vertex().proModel();
    }

    public LlmSyncStatus align(long corpusGeneration) {
        LlmEndpoint flashStatus = ping("flash", flashName, flash);
        LlmEndpoint proStatus = ping("pro", proName, pro);
        LlmSyncStatus status = new LlmSyncStatus(
                corpusGeneration,
                Instant.now(),
                flashStatus.healthy() && proStatus.healthy(),
                List.of(flashStatus, proStatus)
        );
        lastStatus.set(status);
        log.info(
                "LLM sync generation={} flash={} pro={}",
                corpusGeneration,
                flashStatus.healthy(),
                proStatus.healthy()
        );
        return status;
    }

    public LlmSyncStatus status() {
        return lastStatus.get();
    }

    private static LlmEndpoint ping(String role, String model, ChatModel chatModel) {
        try {
            chatModel.chat("Réponds par OK.");
            return new LlmEndpoint(role, model, true, null);
        } catch (RuntimeException ex) {
            log.warn("LLM {} ({}) ping failed: {}", role, model, ex.getMessage());
            return new LlmEndpoint(role, model, false, ex.getMessage());
        }
    }

    public record LlmEndpoint(String role, String model, boolean healthy, String error) {
    }

    public record LlmSyncStatus(
            long corpusGeneration,
            Instant at,
            boolean aligned,
            List<LlmEndpoint> endpoints
    ) {
        static LlmSyncStatus unknown() {
            return new LlmSyncStatus(0, Instant.EPOCH, false, List.of());
        }
    }
}
