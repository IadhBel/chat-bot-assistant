package com.acme.assistant.rag.source;

public record BlobPayload(BlobObject object, byte[] bytes) {
}
