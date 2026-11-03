package com.acme.assistant.rag;

import java.util.List;

public record RetrievedContext(String text, List<RetrievedImage> images, long corpusGeneration) {

    public static RetrievedContext empty(long generation) {
        return new RetrievedContext("", List.of(), generation);
    }

    public record RetrievedImage(String uri, String mimeType, String caption, byte[] bytes) {
    }
}
