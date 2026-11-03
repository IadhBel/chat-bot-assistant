package com.acme.assistant.rag;

import com.acme.assistant.corpus.InMemoryVertexRagPort;
import com.acme.assistant.infrastructure.config.AcmeProperties;
import com.acme.assistant.infrastructure.config.AcmeProperties.Llm;
import com.acme.assistant.infrastructure.config.AcmeProperties.Memory;
import com.acme.assistant.infrastructure.config.AcmeProperties.Rag;
import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Backend;
import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Store;
import com.acme.assistant.infrastructure.config.AcmeProperties.Security;
import com.acme.assistant.rag.source.KnowledgeSourceConfiguration.KnowledgeSourceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeRetrievalServiceVertexTest {

    @Test
    void vertexBackendUsesRagPort() {
        InMemoryVertexRagPort port = new InMemoryVertexRagPort();
        port.seed("gs://kb/megane.md", "megane-e-tech.md", "Mégane E-Tech autonomie 470 km WLTP");
        AcmeProperties properties = new AcmeProperties(
                new Security(false, null),
                new Llm(Llm.Provider.stub, new Llm.Vertex("test", "europe-west1", "f", "p", 0.1, 256, 1, false)),
                new Rag(
                        Backend.vertex,
                        Store.memory,
                        "knowledge_embeddings",
                        32,
                        3,
                        0.0,
                        false,
                        400,
                        40,
                        false,
                        new Rag.Sync(false, Duration.ofHours(1), false),
                        List.of(),
                        new Rag.Vertex("test", "europe-west1", "hash", 1),
                        new Rag.Engine("test", "europe-west1", "docs", "bucket", "knowledge/", "meta/g", null)
                ),
                new Memory(8)
        );

        KnowledgeRetrievalService service = new KnowledgeRetrievalService(
                properties,
                port,
                emptyProvider(),
                emptyProvider(),
                emptyProvider(),
                new KnowledgeSourceRegistry(List.of())
        );

        RetrievedContext context = service.retrieve("autonomie Mégane E-Tech");

        assertThat(context.text()).contains("470");
        assertThat(context.corpusGeneration()).isPositive();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
