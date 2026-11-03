package com.acme.assistant.corpus;

/**
 * Identity of one RAG Engine corpus. Shared by retrieve and import so chat,
 * MCP and the indexer cannot silently point at different resources.
 */
public record VertexRagSettings(
        String project,
        String location,
        String corpus,
        int topK,
        int chunkSize,
        int chunkOverlap,
        String gcsBucket,
        String gcsPrefix,
        String generationObject,
        String layoutParserProcessor
) {

    public VertexRagSettings {
        if (location == null || location.isBlank()) {
            location = "europe-west1";
        }
        if (topK <= 0) {
            topK = 5;
        }
        if (chunkSize <= 0) {
            chunkSize = 512;
        }
        if (chunkOverlap < 0) {
            chunkOverlap = 100;
        }
        if (gcsPrefix == null) {
            gcsPrefix = "knowledge/";
        }
        if (generationObject == null || generationObject.isBlank()) {
            generationObject = "meta/corpus-generation";
        }
    }

    public String locationParent() {
        requireProject();
        return "projects/%s/locations/%s".formatted(project, location);
    }

    public String corpusName() {
        requireProject();
        if (corpus == null || corpus.isBlank()) {
            throw new IllegalStateException("RAG_CORPUS is required when using Vertex RAG Engine");
        }
        if (corpus.startsWith("projects/")) {
            return corpus;
        }
        return "projects/%s/locations/%s/ragCorpora/%s".formatted(project, location, corpus);
    }

    public String regionalEndpoint() {
        return location + "-aiplatform.googleapis.com:443";
    }

    public String importGcsUri() {
        if (gcsBucket == null || gcsBucket.isBlank()) {
            throw new IllegalStateException("GCS_BUCKET is required to import into Vertex RAG Engine");
        }
        String prefix = gcsPrefix.endsWith("/") ? gcsPrefix : gcsPrefix + "/";
        return "gs://" + gcsBucket + "/" + prefix;
    }

    public boolean layoutParserEnabled() {
        return layoutParserProcessor != null && !layoutParserProcessor.isBlank();
    }

    private void requireProject() {
        if (project == null || project.isBlank()) {
            throw new IllegalStateException("GCP_PROJECT is required when using Vertex RAG Engine");
        }
    }
}
