package com.acme.assistant.rag.sync;

import com.acme.assistant.infrastructure.config.AcmeProperties;
import com.acme.assistant.rag.KnowledgeIngestionService;
import com.acme.assistant.rag.KnowledgeIngestionService.IngestResult;
import com.acme.assistant.rag.index.MemoryObjectIndex;
import com.acme.assistant.rag.source.BlobObject;
import com.acme.assistant.rag.source.BlobPayload;
import com.acme.assistant.rag.source.KnowledgeSource;
import com.acme.assistant.rag.source.KnowledgeSourceConfiguration.KnowledgeSourceRegistry;
import com.acme.assistant.rag.MediaKind;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeSyncServiceTest {

    @Test
    void ingestsNewBlobsThenAlignsLlms() {
        BlobObject object = new BlobObject(
                "fs",
                "file:/tmp/notice.pdf",
                "notice.pdf",
                "application/pdf",
                "abc",
                MediaKind.TEXT
        );
        KnowledgeSource source = mock(KnowledgeSource.class);
        when(source.list()).thenReturn(List.of(object));
        when(source.load(object.uri())).thenReturn(Optional.of(new BlobPayload(
                object,
                "garantie 3 ans".getBytes(StandardCharsets.UTF_8)
        )));

        KnowledgeIngestionService ingestion = mock(KnowledgeIngestionService.class);
        when(ingestion.ingestBlob(any())).thenReturn(Optional.of(new IngestResult(1, 2, 0, 1)));
        when(ingestion.corpusGeneration()).thenReturn(1L);

        LlmCorpusSynchronizer llm = mock(LlmCorpusSynchronizer.class);
        AcmeProperties properties = mock(AcmeProperties.class);
        AcmeProperties.Rag rag = mock(AcmeProperties.Rag.class);
        when(properties.rag()).thenReturn(rag);
        when(rag.ingestOnStartup()).thenReturn(false);
        when(rag.sync()).thenReturn(new AcmeProperties.Rag.Sync(false, java.time.Duration.ofHours(1), false));

        KnowledgeSyncService sync = new KnowledgeSyncService(
                new KnowledgeSourceRegistry(List.of(source)),
                ingestion,
                new MemoryObjectIndex(),
                llm,
                properties
        );

        KnowledgeSyncService.SyncReport report = sync.sync("test");
        assertThat(report.ingested()).isEqualTo(1);
        assertThat(report.seen()).isEqualTo(1);
    }
}
