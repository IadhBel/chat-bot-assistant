package com.acme.assistant.mcp.tools;

import com.acme.assistant.corpus.InMemoryVertexRagPort;
import com.acme.assistant.mcp.config.McpProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeToolsTest {

    @Test
    void searchReturnsBoundedSnippet() {
        InMemoryVertexRagPort port = new InMemoryVertexRagPort();
        port.seed("gs://kb/megane.md", "megane-e-tech.md", "Mégane E-Tech autonomie 470 km " + "x".repeat(2000));
        KnowledgeTools tools = new KnowledgeTools(port, properties());

        List<KnowledgeTools.KnowledgeHit> hits = tools.searchKnowledge("autonomie Mégane", 3);

        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().snippet()).endsWith("…");
        assertThat(hits.getFirst().snippet().length()).isLessThanOrEqualTo(81);
        assertThat(hits.getFirst().corpusGeneration()).isPositive();
    }

    @Test
    void blankQueryIsRejected() {
        KnowledgeTools tools = new KnowledgeTools(new InMemoryVertexRagPort(), properties());
        assertThatThrownBy(() -> tools.searchKnowledge("  ", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listSourcesIncludesSeededFiles() {
        InMemoryVertexRagPort port = new InMemoryVertexRagPort();
        port.seed("gs://kb/a.md", "a.md", "batterie");
        KnowledgeTools tools = new KnowledgeTools(port, properties());

        KnowledgeTools.CorpusSnapshot snapshot = tools.listSources();

        assertThat(snapshot.sources()).extracting(KnowledgeTools.SourceRef::displayName).contains("a.md");
    }

    private static McpProperties properties() {
        return new McpProperties(
                new McpProperties.Security(false, null),
                McpProperties.Provider.stub,
                8,
                20,
                80,
                new McpProperties.VertexRag("test", "europe-west1", "docs", "", "knowledge/", "meta/g")
        );
    }
}
