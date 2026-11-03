package com.acme.assistant.infrastructure.llm;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline embedding: stable, cheap, not semantic. Production retrieval must
 * use Vertex. Pair this with lexical search so local RAG still returns the
 * right chunks.
 */
public final class HashEmbeddingModel extends DimensionAwareEmbeddingModel {

    private final int dimension;

    public HashEmbeddingModel(int dimension) {
        if (dimension <= 0) {
            throw new IllegalArgumentException("dimension must be positive");
        }
        this.dimension = dimension;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<Embedding> embeddings = new ArrayList<>(textSegments.size());
        for (TextSegment segment : textSegments) {
            embeddings.add(Embedding.from(expand(segment.text())));
        }
        return Response.from(embeddings);
    }

    private float[] expand(String text) {
        float[] vector = new float[dimension];
        int written = 0;
        int round = 0;
        while (written < vector.length) {
            byte[] digest = sha256(round + ":" + text.strip().toLowerCase());
            for (int i = 0; i < digest.length && written < vector.length; i++) {
                vector[written++] = (digest[i] + 128) / 255.0f * 2.0f - 1.0f;
            }
            round++;
        }
        return vector;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
