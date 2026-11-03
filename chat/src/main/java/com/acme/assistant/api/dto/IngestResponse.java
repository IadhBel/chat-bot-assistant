package com.acme.assistant.api.dto;

public record IngestResponse(int documents, int segments, int images, long corpusGeneration) {
}
