package com.acme.assistant.indexer.index;

import com.acme.assistant.corpus.InMemoryVertexRagPort;
import com.acme.assistant.corpus.VertexRagSettings;
import com.acme.assistant.indexer.config.IndexerProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndexServiceTest {

    @Test
    void stubRunIncrementsGenerationWithoutGcs() {
        IndexerProperties properties = new IndexerProperties(
                IndexerProperties.Provider.stub,
                "",
                false,
                new IndexerProperties.VertexRag(
                        "test", "europe-west1", "docs", "", "knowledge/",
                        "meta/corpus-generation", "", 512, 100
                )
        );
        VertexRagSettings settings = new VertexRagSettings(
                "test", "europe-west1", "docs", 5, 512, 100,
                "", "knowledge/", "meta/corpus-generation", null
        );
        InMemoryVertexRagPort port = new InMemoryVertexRagPort();
        port.seed("gs://kb/megane.md", "megane", "Mégane E-Tech");
        IndexService service = new IndexService(new GcsCorpusPublisher(properties, settings), port, settings);

        IndexRunStats stats = service.run();

        assertThat(stats.failed()).isZero();
        assertThat(stats.corpusGeneration()).isPositive();
    }
}
