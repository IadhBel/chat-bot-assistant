package com.acme.assistant.corpus;

/**
 * Shared contract over one Vertex RAG Engine corpus.
 *
 * <p>Chat and MCP retrieve. The indexer imports. All three must see the same
 * {@link #corpusGeneration()} so a client can tell whether the index moved.
 */
public interface VertexRagPort extends AutoCloseable {

    java.util.List<RagHit> retrieve(String query, int topK);

    ImportResult importGcsPrefix(String gcsUri);

    long corpusGeneration();

    java.util.List<RagFileInfo> listFiles();

    @Override
    default void close() {
    }
}
