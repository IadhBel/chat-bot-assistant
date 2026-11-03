package com.acme.assistant.indexer.index;

import com.acme.assistant.corpus.ImportResult;
import com.acme.assistant.corpus.VertexRagPort;
import com.acme.assistant.corpus.VertexRagSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IndexService {

    private static final Logger log = LoggerFactory.getLogger(IndexService.class);

    private final GcsCorpusPublisher publisher;
    private final VertexRagPort vertexRag;
    private final VertexRagSettings settings;

    public IndexService(
            GcsCorpusPublisher publisher,
            VertexRagPort vertexRag,
            VertexRagSettings settings
    ) {
        this.publisher = publisher;
        this.vertexRag = vertexRag;
        this.settings = settings;
    }

    public IndexRunStats run() {
        int uploaded = publisher.publish();
        ImportResult imported = vertexRag.importGcsPrefix(settings.gcsBucket() == null || settings.gcsBucket().isBlank()
                ? "gs://stub/knowledge/"
                : settings.importGcsUri());
        if (imported.failed() > 0) {
            throw new IllegalStateException(
                    "Vertex RAG import reported " + imported.failed() + " failed files");
        }
        log.info("Index run uploaded={} imported={} skipped={} generation={}",
                uploaded, imported.imported(), imported.skipped(), imported.corpusGeneration());
        return new IndexRunStats(
                uploaded,
                imported.imported(),
                imported.failed(),
                imported.skipped(),
                imported.corpusGeneration()
        );
    }
}
