package com.acme.assistant.rag;

import com.acme.assistant.corpus.RagHit;
import com.acme.assistant.corpus.VertexRagPort;
import com.acme.assistant.infrastructure.config.AcmeProperties;
import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Backend;
import com.acme.assistant.rag.source.KnowledgeSourceConfiguration.KnowledgeSourceRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class KnowledgeRetrievalService {

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> STOPWORDS = Set.of(
            "le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", "à", "a",
            "en", "pour", "par", "sur", "est", "quel", "quelle", "quelles", "quels",
            "the", "of", "and", "d"
    );
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif", ".tif", ".tiff");

    private final Backend backend;
    private final VertexRagPort vertexRag;
    private final ContentRetriever denseRetriever;
    private final KnowledgeIngestionService ingestionService;
    private final KnowledgeSourceRegistry sourceRegistry;
    private final int maxResults;
    private final boolean multimodal;

    public KnowledgeRetrievalService(
            AcmeProperties properties,
            VertexRagPort vertexRag,
            ObjectProvider<EmbeddingModel> queryEmbeddingModel,
            ObjectProvider<EmbeddingStore<TextSegment>> embeddingStore,
            ObjectProvider<KnowledgeIngestionService> ingestionService,
            KnowledgeSourceRegistry sourceRegistry
    ) {
        this.backend = properties.rag().backend();
        this.vertexRag = vertexRag;
        this.sourceRegistry = sourceRegistry;
        this.maxResults = properties.rag().maxResults();
        this.multimodal = properties.rag().multimodal();
        this.ingestionService = ingestionService.getIfAvailable();
        if (backend == Backend.vertex) {
            this.denseRetriever = null;
        } else {
            this.denseRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingModel(require(queryEmbeddingModel, "queryEmbeddingModel"))
                    .embeddingStore(require(embeddingStore, "embeddingStore"))
                    .maxResults(properties.rag().maxResults())
                    .minScore(properties.rag().minScore())
                    .build();
        }
    }

    public RetrievedContext retrieve(String question) {
        if (backend == Backend.vertex) {
            return fromVertex(question);
        }
        return fromLocalStore(question);
    }

    private RetrievedContext fromVertex(String question) {
        List<RagHit> hits = vertexRag.retrieve(question, maxResults);
        if (hits.isEmpty()) {
            return RetrievedContext.empty(vertexRag.corpusGeneration());
        }
        StringBuilder context = new StringBuilder();
        List<RetrievedContext.RetrievedImage> images = new ArrayList<>();
        int index = 1;
        for (RagHit hit : hits) {
            context.append('[').append(index++).append("] ");
            if (!hit.sourceDisplayName().isBlank()) {
                context.append('(').append(hit.sourceDisplayName()).append(") ");
            } else if (!hit.sourceUri().isBlank()) {
                context.append('(').append(hit.sourceUri()).append(") ");
            }
            if (looksLikeImage(hit.sourceUri())) {
                context.append("[IMAGE] ");
            }
            context.append(hit.text().strip()).append("\n\n");
            if (multimodal && looksLikeImage(hit.sourceUri())) {
                sourceRegistry.load(hit.sourceUri()).ifPresent(payload -> images.add(
                        new RetrievedContext.RetrievedImage(
                                hit.sourceUri(),
                                payload.object().contentType(),
                                null,
                                payload.bytes()
                        )));
            }
        }
        return new RetrievedContext(
                context.toString().strip(),
                List.copyOf(images),
                vertexRag.corpusGeneration()
        );
    }

    private RetrievedContext fromLocalStore(String question) {
        List<Content> merged = merge(denseRetriever.retrieve(Query.from(question)), lexical(question));
        if (merged.isEmpty()) {
            return RetrievedContext.empty(localGeneration());
        }
        StringBuilder context = new StringBuilder();
        List<RetrievedContext.RetrievedImage> images = new ArrayList<>();
        int index = 1;
        for (Content content : merged) {
            TextSegment segment = content.textSegment();
            String source = segment.metadata().getString("source");
            String uri = segment.metadata().getString("uri");
            String modality = segment.metadata().getString("modality");
            context.append('[').append(index++).append("] ");
            if (source != null) {
                context.append('(').append(source).append(") ");
            }
            if (MediaKind.IMAGE.name().equals(modality)) {
                context.append("[IMAGE] ");
            }
            context.append(segment.text().strip()).append("\n\n");
            if (multimodal && MediaKind.IMAGE.name().equals(modality) && uri != null) {
                sourceRegistry.load(uri).ifPresent(payload -> images.add(new RetrievedContext.RetrievedImage(
                        uri,
                        payload.object().contentType() == null
                                ? segment.metadata().getString("mimeType")
                                : payload.object().contentType(),
                        segment.metadata().getString("caption"),
                        payload.bytes()
                )));
            }
        }
        return new RetrievedContext(
                context.toString().strip(),
                List.copyOf(images),
                localGeneration()
        );
    }

    private List<Content> lexical(String question) {
        if (ingestionService == null) {
            return List.of();
        }
        Set<String> queryTokens = tokens(question);
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        record Scored(TextSegment segment, long score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (TextSegment segment : ingestionService.indexedSegments()) {
            Set<String> segmentTokens = tokens(segment.text());
            long overlap = queryTokens.stream().filter(segmentTokens::contains).count();
            if (overlap > 0) {
                scored.add(new Scored(segment, overlap));
            }
        }
        scored.sort((left, right) -> Long.compare(right.score(), left.score()));
        return scored.stream()
                .limit(maxResults)
                .map(item -> Content.from(item.segment()))
                .toList();
    }

    private long localGeneration() {
        return ingestionService == null ? 0L : ingestionService.corpusGeneration();
    }

    private static List<Content> merge(List<Content> dense, List<Content> lexical) {
        Map<String, Content> unique = new LinkedHashMap<>();
        for (Content content : dense) {
            unique.putIfAbsent(content.textSegment().text(), content);
        }
        for (Content content : lexical) {
            unique.putIfAbsent(content.textSegment().text(), content);
        }
        return List.copyOf(unique.values());
    }

    private static Set<String> tokens(String text) {
        return TOKEN.matcher(text.toLowerCase(Locale.ROOT)).results()
                .map(match -> match.group())
                .filter(token -> token.length() > 1 && !STOPWORDS.contains(token))
                .collect(Collectors.toSet());
    }

    private static boolean looksLikeImage(String uri) {
        if (uri == null) {
            return false;
        }
        String lower = uri.toLowerCase(Locale.ROOT);
        int query = lower.indexOf('?');
        String path = query >= 0 ? lower.substring(0, query) : lower;
        return IMAGE_EXTENSIONS.stream().anyMatch(path::endsWith);
    }

    private static <T> T require(ObjectProvider<T> provider, String name) {
        T bean = provider.getIfAvailable();
        if (bean == null) {
            throw new IllegalStateException(name + " is required when acme.rag.backend=local");
        }
        return bean;
    }
}
