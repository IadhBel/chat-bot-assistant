package com.acme.assistant.infrastructure.rag;

import com.acme.assistant.infrastructure.config.AcmeProperties;
import com.acme.assistant.infrastructure.config.AcmeProperties.Llm.Provider;
import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Store;
import com.acme.assistant.infrastructure.llm.HashEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "acme.rag.backend", havingValue = "local", matchIfMissing = true)
public class EmbeddingConfiguration {

    @Bean
    @Primary
    EmbeddingModel queryEmbeddingModel(AcmeProperties properties) {
        return embeddingModel(properties, VertexAiEmbeddingModel.TaskType.RETRIEVAL_QUERY);
    }

    @Bean
    EmbeddingModel documentEmbeddingModel(AcmeProperties properties) {
        return embeddingModel(properties, VertexAiEmbeddingModel.TaskType.RETRIEVAL_DOCUMENT);
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(
            AcmeProperties properties,
            ObjectProvider<DataSource> dataSource
    ) {
        if (properties.rag().store() == Store.memory) {
            return new InMemoryEmbeddingStore<>();
        }
        DataSource source = dataSource.getIfAvailable();
        if (source == null) {
            throw new IllegalStateException("A DataSource is required when acme.rag.store=pgvector");
        }
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(source)
                .table(properties.rag().table())
                .dimension(properties.rag().dimension())
                .createTable(true)
                .dropTableFirst(false)
                .build();
    }

    private static EmbeddingModel embeddingModel(
            AcmeProperties properties,
            VertexAiEmbeddingModel.TaskType taskType
    ) {
        if (properties.llm().provider() == Provider.stub) {
            return new HashEmbeddingModel(properties.rag().dimension());
        }
        AcmeProperties.Rag.Vertex vertex = properties.rag().vertex();
        if (vertex.project() == null || vertex.project().isBlank()) {
            throw new IllegalStateException("GCP_PROJECT is required for Vertex embeddings");
        }
        return VertexAiEmbeddingModel.builder()
                .project(vertex.project())
                .location(vertex.location())
                .endpoint(vertex.location() + "-aiplatform.googleapis.com:443")
                .publisher("google")
                .modelName(vertex.model())
                .outputDimensionality(properties.rag().dimension())
                .taskType(taskType)
                .maxRetries(vertex.maxRetries())
                .autoTruncate(false)
                .build();
    }
}
