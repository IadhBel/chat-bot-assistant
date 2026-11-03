package com.acme.assistant.rag;

import com.acme.assistant.infrastructure.config.AcmeProperties;
import com.acme.assistant.rag.index.IndexedObject;
import com.acme.assistant.rag.index.ObjectIndex;
import com.acme.assistant.rag.parse.ImageCaptioner;
import com.acme.assistant.rag.parse.MediaDocumentParser;
import com.acme.assistant.rag.source.BlobPayload;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;

@Service
@ConditionalOnProperty(name = "acme.rag.backend", havingValue = "local", matchIfMissing = true)
public class KnowledgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final EmbeddingStoreIngestor ingestor;
    private final MediaDocumentParser parser;
    private final ImageCaptioner captioner;
    private final ObjectIndex objectIndex;
    private final int chunkSize;
    private final int chunkOverlap;
    private final boolean multimodal;
    private final CopyOnWriteArrayList<TextSegment> indexedSegments = new CopyOnWriteArrayList<>();
    private final AtomicLong corpusGeneration = new AtomicLong(0);

    public KnowledgeIngestionService(
            AcmeProperties properties,
            @Qualifier("documentEmbeddingModel") EmbeddingModel documentEmbeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            MediaDocumentParser parser,
            ImageCaptioner captioner,
            ObjectIndex objectIndex
    ) {
        this.parser = parser;
        this.captioner = captioner;
        this.objectIndex = objectIndex;
        this.chunkSize = properties.rag().chunkSize();
        this.chunkOverlap = properties.rag().chunkOverlap();
        this.multimodal = properties.rag().multimodal();
        this.ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(documentEmbeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(recursive(chunkSize, chunkOverlap))
                .build();
    }

    public IngestResult ingest(List<KnowledgeDocument> documents) {
        List<Document> langchainDocuments = new ArrayList<>();
        List<TextSegment> segments = new ArrayList<>();
        for (KnowledgeDocument document : documents) {
            Metadata metadata = new Metadata()
                    .put("source", document.source())
                    .put("uri", "inline:" + document.source())
                    .put("modality", MediaKind.TEXT.name());
            Document langchainDocument = Document.from(document.text(), metadata);
            langchainDocuments.add(langchainDocument);
            segments.addAll(recursive(chunkSize, chunkOverlap).split(langchainDocument));
        }
        if (!langchainDocuments.isEmpty()) {
            ingestor.ingest(langchainDocuments);
            indexedSegments.addAll(segments);
            corpusGeneration.incrementAndGet();
        }
        return new IngestResult(documents.size(), segments.size(), 0, corpusGeneration.get());
    }

    public Optional<IngestResult> ingestBlob(BlobPayload payload) {
        Optional<String> known = objectIndex.fingerprint(payload.object().uri());
        if (known.isPresent() && known.get().equals(payload.object().fingerprint())) {
            return Optional.empty();
        }
        return switch (payload.object().kind()) {
            case TEXT -> Optional.of(ingestText(payload));
            case IMAGE -> multimodal ? Optional.of(ingestImage(payload)) : Optional.empty();
            case UNSUPPORTED -> Optional.empty();
        };
    }

    private IngestResult ingestText(BlobPayload payload) {
        Document document = parser.parseText(payload).orElse(null);
        if (document == null || document.text() == null || document.text().isBlank()) {
            log.warn("Empty text after parse: {}", payload.object().uri());
            return new IngestResult(0, 0, 0, corpusGeneration.get());
        }
        List<TextSegment> segments = recursive(chunkSize, chunkOverlap).split(document);
        ingestor.ingest(document);
        indexedSegments.addAll(segments);
        objectIndex.put(new IndexedObject(
                payload.object().uri(),
                payload.object().fingerprint(),
                MediaKind.TEXT.name()
        ));
        long generation = corpusGeneration.incrementAndGet();
        return new IngestResult(1, segments.size(), 0, generation);
    }

    private IngestResult ingestImage(BlobPayload payload) {
        String caption = captioner.caption(payload);
        Metadata metadata = MediaDocumentParser.baseMetadata(payload)
                .put("caption", caption)
                .put("mimeType", ImageCaptioner.mimeType(payload));
        Document document = Document.from(
                "Image: " + payload.object().path() + "\n" + caption,
                metadata
        );
        List<TextSegment> segments = recursive(chunkSize, chunkOverlap).split(document);
        ingestor.ingest(document);
        indexedSegments.addAll(segments);
        objectIndex.put(new IndexedObject(
                payload.object().uri(),
                payload.object().fingerprint(),
                MediaKind.IMAGE.name()
        ));
        long generation = corpusGeneration.incrementAndGet();
        return new IngestResult(1, segments.size(), 1, generation);
    }

    public void remove(String uri) {
        objectIndex.remove(uri);
        indexedSegments.removeIf(segment -> uri.equals(segment.metadata().getString("uri")));
        corpusGeneration.incrementAndGet();
    }

    List<TextSegment> indexedSegments() {
        return List.copyOf(indexedSegments);
    }

    public long corpusGeneration() {
        return corpusGeneration.get();
    }

    public record IngestResult(int documents, int segments, int images, long corpusGeneration) {
    }
}
