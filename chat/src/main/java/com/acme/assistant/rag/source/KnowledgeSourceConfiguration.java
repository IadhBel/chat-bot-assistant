package com.acme.assistant.rag.source;

import com.acme.assistant.infrastructure.config.AcmeProperties;
import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Configuration
public class KnowledgeSourceConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSourceConfiguration.class);

    @Bean
    List<KnowledgeSource> knowledgeSources(AcmeProperties properties) {
        List<KnowledgeSource> sources = new ArrayList<>();
        for (Source spec : properties.rag().sources()) {
            if (!spec.enabled()) {
                continue;
            }
            try {
                sources.add(create(spec));
                log.info("Knowledge source enabled: {} ({})", spec.id(), spec.type());
            } catch (RuntimeException ex) {
                log.warn("Skipping knowledge source {} ({}): {}", spec.id(), spec.type(), ex.getMessage());
            }
        }
        return List.copyOf(sources);
    }

    @Bean
    KnowledgeSourceRegistry knowledgeSourceRegistry(List<KnowledgeSource> knowledgeSources) {
        return new KnowledgeSourceRegistry(knowledgeSources);
    }

    private static KnowledgeSource create(Source spec) {
        return switch (spec.type()) {
            case classpath -> new ClasspathKnowledgeSource(spec);
            case filesystem -> new FileSystemKnowledgeSource(spec);
            case gcs -> new GcsKnowledgeSource(spec);
            case s3 -> new S3KnowledgeSource(spec);
            case azure -> new AzureBlobKnowledgeSource(spec);
        };
    }

    public static final class KnowledgeSourceRegistry {

        private final List<KnowledgeSource> sources;

        public KnowledgeSourceRegistry(List<KnowledgeSource> sources) {
            this.sources = sources;
        }

        public List<KnowledgeSource> sources() {
            return sources;
        }

        public Optional<BlobPayload> load(String uri) {
            return sources.stream()
                    .filter(source -> source.owns(uri))
                    .map(source -> source.load(uri))
                    .flatMap(Optional::stream)
                    .findFirst();
        }
    }
}
