package com.acme.assistant.mcp.tools;

import com.acme.assistant.corpus.RagFileInfo;
import com.acme.assistant.corpus.RagHit;
import com.acme.assistant.corpus.VertexRagPort;
import com.acme.assistant.mcp.config.McpProperties;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Knowledge MCP surface over the same Vertex RAG corpus as chat.
 *
 * <p>Tools return pointers and bounded snippets, never a full document. The
 * intended sequence is {@code list_sources} then {@code search_knowledge}.
 */
@Service
public class KnowledgeTools {

    private final VertexRagPort vertexRag;
    private final McpProperties properties;

    public KnowledgeTools(VertexRagPort vertexRag, McpProperties properties) {
        this.vertexRag = vertexRag;
        this.properties = properties;
    }

    @McpTool(name = "list_sources", description = """
            List files currently in the Acme knowledge corpus, with the corpus
            generation number. Call this first to confirm the index is populated.
            Cost: about 50 tokens.""")
    public CorpusSnapshot listSources() {
        List<RagFileInfo> files = vertexRag.listFiles();
        return new CorpusSnapshot(
                vertexRag.corpusGeneration(),
                files.stream()
                        .map(file -> new SourceRef(file.displayName(), file.sourceUri()))
                        .toList()
        );
    }

    @McpTool(name = "search_knowledge", description = """
            Semantic retrieve over Acme product, after-sales and network
            documents indexed in Vertex RAG Engine. Use this instead of guessing
            vehicle facts. Returns rank-ordered snippets, truncated on purpose:
            ask again with a narrower query rather than requesting many hits.
            Cost: roughly 150 tokens per hit.""")
    public List<KnowledgeHit> searchKnowledge(
            @McpToolParam(description = "Natural-language question, e.g. autonomie Mégane E-Tech.",
                    required = true)
            String query,
            @McpToolParam(description = "Maximum hits. Defaults to 8; keep it small.", required = false)
            Integer limit) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be empty");
        }
        int topK = properties.clampLimit(limit);
        List<RagHit> hits = vertexRag.retrieve(query, topK);
        return hits.stream()
                .map(hit -> new KnowledgeHit(
                        hit.sourceDisplayName().isBlank() ? hit.sourceUri() : hit.sourceDisplayName(),
                        hit.sourceUri(),
                        snippet(hit.text()),
                        hit.score(),
                        vertexRag.corpusGeneration()
                ))
                .toList();
    }

    private String snippet(String text) {
        String stripped = text == null ? "" : text.strip();
        int max = properties.snippetChars();
        if (stripped.length() <= max) {
            return stripped;
        }
        return stripped.substring(0, max) + "…";
    }

    public record CorpusSnapshot(long corpusGeneration, List<SourceRef> sources) {
    }

    public record SourceRef(String displayName, String sourceUri) {
    }

    public record KnowledgeHit(
            String source,
            String sourceUri,
            String snippet,
            double score,
            long corpusGeneration
    ) {
    }
}
