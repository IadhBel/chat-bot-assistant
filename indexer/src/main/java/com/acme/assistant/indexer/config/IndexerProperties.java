package com.acme.assistant.indexer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "acme.indexer")
public record IndexerProperties(
        Provider provider,
        String corpusPath,
        boolean uploadClasspathSeed,
        VertexRag engine
) {

    public enum Provider {
        vertex,
        stub
    }

    public Provider provider() {
        return provider == null ? Provider.stub : provider;
    }

    public VertexRag engine() {
        return engine == null
                ? new VertexRag(null, "europe-west1", null, null, "knowledge/", "meta/corpus-generation", null, 512, 100)
                : engine;
    }

    public record VertexRag(
            String project,
            String location,
            String corpus,
            String gcsBucket,
            String gcsPrefix,
            String generationObject,
            String layoutParserProcessor,
            int chunkSize,
            int chunkOverlap
    ) {
        public VertexRag {
            if (location == null || location.isBlank()) {
                location = "europe-west1";
            }
            if (gcsPrefix == null) {
                gcsPrefix = "knowledge/";
            }
            if (generationObject == null || generationObject.isBlank()) {
                generationObject = "meta/corpus-generation";
            }
            if (chunkSize <= 0) {
                chunkSize = 512;
            }
            if (chunkOverlap < 0) {
                chunkOverlap = 100;
            }
        }
    }
}
