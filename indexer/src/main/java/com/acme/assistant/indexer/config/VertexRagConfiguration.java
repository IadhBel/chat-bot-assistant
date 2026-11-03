package com.acme.assistant.indexer.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.acme.assistant.corpus.GcsCorpusGenerationStore;
import com.acme.assistant.corpus.InMemoryVertexRagPort;
import com.acme.assistant.corpus.VertexRagEngineClient;
import com.acme.assistant.corpus.VertexRagPort;
import com.acme.assistant.corpus.VertexRagSettings;
import com.acme.assistant.indexer.config.IndexerProperties.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexRagConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VertexRagConfiguration.class);

    @Bean
    VertexRagSettings vertexRagSettings(IndexerProperties properties) {
        IndexerProperties.VertexRag engine = properties.engine();
        return new VertexRagSettings(
                engine.project(),
                engine.location(),
                engine.corpus(),
                5,
                engine.chunkSize(),
                engine.chunkOverlap(),
                engine.gcsBucket(),
                engine.gcsPrefix(),
                engine.generationObject(),
                engine.layoutParserProcessor()
        );
    }

    @Bean(destroyMethod = "close")
    VertexRagPort vertexRagPort(IndexerProperties properties, VertexRagSettings settings) {
        if (properties.provider() != Provider.vertex) {
            log.info("Indexer Vertex RAG port: in-memory stub");
            return new InMemoryVertexRagPort();
        }
        log.info("Indexer Vertex RAG port: engine endpoint={} corpus={}",
                settings.regionalEndpoint(), settings.corpusName());
        Storage storage = StorageOptions.newBuilder()
                .setProjectId(settings.project())
                .build()
                .getService();
        return new VertexRagEngineClient(settings, new GcsCorpusGenerationStore(storage, settings));
    }
}
