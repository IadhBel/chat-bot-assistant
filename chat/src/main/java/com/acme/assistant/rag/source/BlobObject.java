package com.acme.assistant.rag.source;

import com.acme.assistant.rag.MediaKind;

public record BlobObject(
        String sourceId,
        String uri,
        String path,
        String contentType,
        String fingerprint,
        MediaKind kind
) {
}
