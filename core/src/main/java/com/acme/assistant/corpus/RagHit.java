package com.acme.assistant.corpus;

public record RagHit(String sourceUri, String sourceDisplayName, String text, double score) {

    public RagHit {
        sourceUri = sourceUri == null ? "" : sourceUri;
        sourceDisplayName = sourceDisplayName == null ? "" : sourceDisplayName;
        text = text == null ? "" : text;
    }
}
