package com.acme.assistant.corpus;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVertexRagPortTest {

    @Test
    void retrieveRanksByLexicalOverlap() {
        InMemoryVertexRagPort port = new InMemoryVertexRagPort();
        port.seed("gs://kb/megane.md", "megane", "Mégane E-Tech autonomie 470 km WLTP");
        port.seed("gs://kb/clio.md", "clio", "Clio essence ville parking");

        List<RagHit> hits = port.retrieve("autonomie Mégane E-Tech", 3);

        assertThat(hits).isNotEmpty();
        assertThat(hits.getFirst().sourceUri()).isEqualTo("gs://kb/megane.md");
        assertThat(hits.getFirst().text()).contains("470");
    }

    @Test
    void importIncrementsGeneration() {
        InMemoryVertexRagPort port = new InMemoryVertexRagPort();
        port.seed("gs://kb/a.md", "a", "batterie 60 kWh");
        long before = port.corpusGeneration();

        ImportResult result = port.importGcsPrefix("gs://kb/knowledge/");

        assertThat(result.corpusGeneration()).isEqualTo(before + 1);
        assertThat(port.listFiles()).hasSize(1);
    }
}
