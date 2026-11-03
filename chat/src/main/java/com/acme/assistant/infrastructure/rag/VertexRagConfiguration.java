package com.acme.assistant.infrastructure.rag;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.acme.assistant.corpus.GcsCorpusGenerationStore;
import com.acme.assistant.corpus.InMemoryVertexRagPort;
import com.acme.assistant.corpus.VertexRagEngineClient;
import com.acme.assistant.corpus.VertexRagPort;
import com.acme.assistant.corpus.VertexRagSettings;
import com.acme.assistant.infrastructure.config.AcmeProperties;
import com.acme.assistant.infrastructure.config.AcmeProperties.Llm.Provider;
import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Backend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexRagConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VertexRagConfiguration.class);

    @Bean
    VertexRagSettings vertexRagSettings(AcmeProperties properties) {
        AcmeProperties.Rag.Engine engine = properties.rag().engine();
        String project = engine.project() == null || engine.project().isBlank()
                ? properties.llm().vertex().project()
                : engine.project();
        String location = engine.location() == null || engine.location().isBlank()
                ? properties.llm().vertex().location()
                : engine.location();
        return new VertexRagSettings(
                project,
                location,
                engine.corpus(),
                properties.rag().maxResults(),
                properties.rag().chunkSize(),
                properties.rag().chunkOverlap(),
                engine.gcsBucket(),
                engine.gcsPrefix(),
                engine.generationObject(),
                engine.layoutParserProcessor()
        );
    }

    @Bean(destroyMethod = "closeIfNeeded")
    CloseableVertexRagPort vertexRagPort(AcmeProperties properties, VertexRagSettings settings) {
        boolean vertexLive = properties.rag().backend() == Backend.vertex
                && properties.llm().provider() == Provider.vertex;
        if (!vertexLive) {
            log.info("Vertex RAG port: in-memory stub (backend={}, llm={})",
                    properties.rag().backend(), properties.llm().provider());
            return new CloseableVertexRagPort(new InMemoryVertexRagPort(), null);
        }
        log.info("Vertex RAG port: engine endpoint={} corpus={}",
                settings.regionalEndpoint(), settings.corpusName());
        Storage storage = StorageOptions.newBuilder()
                .setProjectId(settings.project())
                .build()
                .getService();
        VertexRagEngineClient client = new VertexRagEngineClient(
                settings,
                new GcsCorpusGenerationStore(storage, settings)
        );
        return new CloseableVertexRagPort(client, client);
    }

    /**
     * Spring destroy hook that closes the engine client without forcing
     * {@link AutoCloseable} onto the in-memory stub.
     */
    public record CloseableVertexRagPort(VertexRagPort delegate, AutoCloseable resource)
            implements VertexRagPort {

        @Override
        public java.util.List<com.acme.assistant.corpus.RagHit> retrieve(String query, int topK) {
            return delegate.retrieve(query, topK);
        }

        @Override
        public com.acme.assistant.corpus.ImportResult importGcsPrefix(String gcsUri) {
            return delegate.importGcsPrefix(gcsUri);
        }

        @Override
        public long corpusGeneration() {
            return delegate.corpusGeneration();
        }

        @Override
        public java.util.List<com.acme.assistant.corpus.RagFileInfo> listFiles() {
            return delegate.listFiles();
        }

        public void closeIfNeeded() {
            if (resource == null) {
                return;
            }
            try {
                resource.close();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to close Vertex RAG client", ex);
            }
        }
    }
}
