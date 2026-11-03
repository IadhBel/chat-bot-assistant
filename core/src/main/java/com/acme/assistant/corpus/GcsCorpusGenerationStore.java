package com.acme.assistant.corpus;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Generation marker stored next to the corpus on GCS.
 *
 * <p>Vertex RAG Engine has no monotonically increasing generation of our own,
 * so chat and MCP read the same object the indexer writes after a successful
 * import. That is the contract {@code corpusGeneration} in the SSE meta event.
 */
public final class GcsCorpusGenerationStore {

    private final Storage storage;
    private final BlobId blobId;

    public GcsCorpusGenerationStore(Storage storage, VertexRagSettings settings) {
        this.storage = Objects.requireNonNull(storage, "storage");
        if (settings.gcsBucket() == null || settings.gcsBucket().isBlank()) {
            throw new IllegalStateException("GCS_BUCKET is required for the corpus generation marker");
        }
        this.blobId = BlobId.of(settings.gcsBucket(), settings.generationObject());
    }

    public long read() {
        Blob blob = storage.get(blobId);
        if (blob == null || blob.getContent() == null || blob.getContent().length == 0) {
            return 0L;
        }
        try {
            return Long.parseLong(new String(blob.getContent(), StandardCharsets.UTF_8).trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public long increment() {
        long next = read() + 1;
        storage.create(
                BlobInfo.newBuilder(blobId).setContentType("text/plain").build(),
                Long.toString(next).getBytes(StandardCharsets.UTF_8)
        );
        return next;
    }
}
