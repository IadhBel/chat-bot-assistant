package com.acme.assistant.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "acme")
public record AcmeProperties(
        Security security,
        Llm llm,
        Rag rag,
        Memory memory
) {

    public record Security(boolean enabled, String apiToken) {
    }

    public record Llm(Provider provider, Vertex vertex) {
        public enum Provider {
            vertex,
            stub
        }

        public record Vertex(
                String project,
                String location,
                String flashModel,
                String proModel,
                double temperature,
                int maxOutputTokens,
                int maxRetries,
                boolean logRequests
        ) {
        }
    }

    public record Rag(
            Backend backend,
            Store store,
            String table,
            int dimension,
            int maxResults,
            double minScore,
            boolean ingestOnStartup,
            int chunkSize,
            int chunkOverlap,
            boolean multimodal,
            Sync sync,
            List<Source> sources,
            Vertex vertex,
            Engine engine
    ) {
        public enum Backend {
            local,
            vertex
        }

        public enum Store {
            pgvector,
            memory
        }

        public Backend backend() {
            return backend == null ? Backend.local : backend;
        }

        public Engine engine() {
            return engine == null
                    ? new Engine(null, "europe-west1", null, null, "knowledge/", "meta/corpus-generation", null)
                    : engine;
        }

        public Sync sync() {
            return sync == null ? new Sync(true, Duration.ofMinutes(15), false) : sync;
        }

        public List<Source> sources() {
            if (sources == null || sources.isEmpty()) {
                return List.of(Source.classpathDefault());
            }
            return sources;
        }

        public record Sync(boolean enabled, Duration interval, boolean deleteMissing) {
        }

        public record Source(
                String id,
                Type type,
                boolean enabled,
                String project,
                String bucket,
                String container,
                String prefix,
                String glob,
                String path,
                String region,
                String connectionString
        ) {
            public enum Type {
                classpath,
                filesystem,
                gcs,
                s3,
                azure
            }

            static Source classpathDefault() {
                return new Source(
                        "classpath-kb",
                        Type.classpath,
                        true,
                        null,
                        null,
                        null,
                        "knowledge/",
                        "**/*",
                        null,
                        null,
                        null
                );
            }

            public String prefixOrEmpty() {
                return prefix == null ? "" : prefix;
            }

            public String globOrAll() {
                return glob == null || glob.isBlank() ? "**/*" : glob;
            }
        }

        public record Vertex(String project, String location, String model, int maxRetries) {
        }

        public record Engine(
                String project,
                String location,
                String corpus,
                String gcsBucket,
                String gcsPrefix,
                String generationObject,
                String layoutParserProcessor
        ) {
        }
    }

    public record Memory(int maxMessages) {
    }
}
