package com.acme.assistant.rag.parse;

import com.acme.assistant.rag.MediaKind;
import com.acme.assistant.rag.source.BlobPayload;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "acme.rag.backend", havingValue = "local", matchIfMissing = true)
public class MediaDocumentParser {

    private static final Logger log = LoggerFactory.getLogger(MediaDocumentParser.class);

    private final ApacheTikaDocumentParser tika = new ApacheTikaDocumentParser();

    public Optional<Document> parseText(BlobPayload payload) {
        if (payload.object().kind() != MediaKind.TEXT) {
            return Optional.empty();
        }
        Metadata metadata = baseMetadata(payload);
        try {
            Document document = tika.parse(new ByteArrayInputStream(payload.bytes()));
            return Optional.of(Document.from(document.text(), metadata.merge(document.metadata())));
        } catch (RuntimeException ex) {
            log.warn("Tika failed on {}, falling back to UTF-8: {}", payload.object().uri(), ex.getMessage());
            String text = new String(payload.bytes(), StandardCharsets.UTF_8).strip();
            if (text.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Document.from(text, metadata));
        }
    }

    public static Metadata baseMetadata(BlobPayload payload) {
        return new Metadata()
                .put("source", payload.object().path())
                .put("uri", payload.object().uri())
                .put("sourceId", payload.object().sourceId())
                .put("modality", payload.object().kind().name())
                .put("fingerprint", payload.object().fingerprint());
    }
}
