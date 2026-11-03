package com.acme.assistant.mcp.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.acme.assistant.corpus.GcsCorpusGenerationStore;
import com.acme.assistant.corpus.InMemoryVertexRagPort;
import com.acme.assistant.corpus.VertexRagEngineClient;
import com.acme.assistant.corpus.VertexRagPort;
import com.acme.assistant.corpus.VertexRagSettings;
import com.acme.assistant.mcp.config.McpProperties.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexRagConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VertexRagConfiguration.class);

    @Bean
    VertexRagSettings vertexRagSettings(McpProperties properties) {
        McpProperties.VertexRag engine = properties.engine();
        return new VertexRagSettings(
                engine.project(),
                engine.location(),
                engine.corpus(),
                properties.defaultLimit(),
                512,
                100,
                engine.gcsBucket(),
                engine.gcsPrefix(),
                engine.generationObject(),
                null
        );
    }

    @Bean(destroyMethod = "close")
    VertexRagPort vertexRagPort(McpProperties properties, VertexRagSettings settings) {
        if (properties.provider() != Provider.vertex) {
            log.info("MCP Vertex RAG port: in-memory stub");
            InMemoryVertexRagPort memory = new InMemoryVertexRagPort();
            memory.seed(
                    "classpath:knowledge/megane-e-tech.md",
                    "megane-e-tech.md",
                    "Mégane E-Tech Electric autonomie jusqu'à 470 km WLTP batterie 60 kWh."
            );
            memory.seed(
                    "classpath:knowledge/apres-vente.md",
                    "apres-vente.md",
                    "Entretien Acme révision vidange réseau après-vente."
            );
            return memory;
        }
        log.info("MCP Vertex RAG port: engine endpoint={} corpus={}",
                settings.regionalEndpoint(), settings.corpusName());
        Storage storage = StorageOptions.newBuilder()
                .setProjectId(settings.project())
                .build()
                .getService();
        return new VertexRagEngineClient(settings, new GcsCorpusGenerationStore(storage, settings));
    }
}
