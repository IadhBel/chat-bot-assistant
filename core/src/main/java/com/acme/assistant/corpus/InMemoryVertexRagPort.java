package com.acme.assistant.corpus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Local / test stand-in for Vertex RAG Engine. Lexical overlap, no network.
 */
public final class InMemoryVertexRagPort implements VertexRagPort {

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> STOPWORDS = Set.of(
            "le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", "à", "a",
            "en", "pour", "par", "sur", "est", "quel", "quelle", "quelles", "quels",
            "the", "of", "and", "d"
    );

    private final CopyOnWriteArrayList<RagHit> documents = new CopyOnWriteArrayList<>();
    private final AtomicLong generation = new AtomicLong(0);

    public void seed(String sourceUri, String displayName, String text) {
        documents.add(new RagHit(sourceUri, displayName, text, 1.0));
        generation.incrementAndGet();
    }

    @Override
    public List<RagHit> retrieve(String query, int topK) {
        Set<String> queryTokens = tokens(query == null ? "" : query);
        if (queryTokens.isEmpty() || documents.isEmpty()) {
            return List.of();
        }
        record Scored(RagHit hit, long score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (RagHit document : documents) {
            long overlap = queryTokens.stream().filter(tokens(document.text())::contains).count();
            if (overlap > 0) {
                scored.add(new Scored(document, overlap));
            }
        }
        scored.sort(Comparator.comparingLong(Scored::score).reversed());
        int limit = Math.max(1, topK);
        return scored.stream()
                .limit(limit)
                .map(item -> new RagHit(
                        item.hit().sourceUri(),
                        item.hit().sourceDisplayName(),
                        item.hit().text(),
                        item.score()))
                .toList();
    }

    @Override
    public ImportResult importGcsPrefix(String gcsUri) {
        long next = generation.incrementAndGet();
        return new ImportResult(documents.size(), 0, 0, next);
    }

    @Override
    public long corpusGeneration() {
        return generation.get();
    }

    @Override
    public List<RagFileInfo> listFiles() {
        return documents.stream()
                .map(doc -> new RagFileInfo(doc.sourceUri(), doc.sourceDisplayName(), doc.sourceUri()))
                .toList();
    }

    private static Set<String> tokens(String text) {
        return TOKEN.matcher(text.toLowerCase(Locale.ROOT)).results()
                .map(match -> match.group())
                .filter(token -> token.length() > 1 && !STOPWORDS.contains(token))
                .collect(Collectors.toSet());
    }
}
