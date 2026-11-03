package com.acme.assistant.indexer.index;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.acme.assistant.corpus.VertexRagSettings;
import com.acme.assistant.indexer.config.IndexerProperties;
import com.acme.assistant.indexer.config.IndexerProperties.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Copies seed files into the GCS prefix Vertex will import. No-op in stub mode.
 */
@Component
public class GcsCorpusPublisher {

    private static final Logger log = LoggerFactory.getLogger(GcsCorpusPublisher.class);

    private final IndexerProperties properties;
    private final VertexRagSettings settings;

    public GcsCorpusPublisher(IndexerProperties properties, VertexRagSettings settings) {
        this.properties = properties;
        this.settings = settings;
    }

    public int publish() {
        if (properties.provider() != Provider.vertex) {
            log.info("Skipping GCS publish in stub mode");
            return 0;
        }
        Storage storage = StorageOptions.newBuilder()
                .setProjectId(settings.project())
                .build()
                .getService();
        int uploaded = 0;
        if (properties.uploadClasspathSeed()) {
            uploaded += publishClasspath(storage);
        }
        if (properties.corpusPath() != null && !properties.corpusPath().isBlank()) {
            uploaded += publishDirectory(storage, Path.of(properties.corpusPath()));
        }
        log.info("Published {} objects to {}", uploaded, settings.importGcsUri());
        return uploaded;
    }

    private int publishClasspath(Storage storage) {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:knowledge/**/*");
            int count = 0;
            for (Resource resource : resources) {
                if (!resource.isReadable() || resource.getFilename() == null || resource.getFilename().isBlank()) {
                    continue;
                }
                String objectName = objectName(resource.getFilename());
                try (InputStream in = resource.getInputStream()) {
                    storage.create(
                            BlobInfo.newBuilder(BlobId.of(settings.gcsBucket(), objectName)).build(),
                            in.readAllBytes()
                    );
                    count++;
                }
            }
            return count;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to publish classpath knowledge seed", ex);
        }
    }

    private int publishDirectory(Storage storage, Path root) {
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("CORPUS_PATH is not a directory: " + root);
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return (int) walk.filter(Files::isRegularFile).peek(file -> {
                String relative = root.relativize(file).toString().replace('\\', '/');
                try {
                    storage.create(
                            BlobInfo.newBuilder(BlobId.of(settings.gcsBucket(), objectName(relative))).build(),
                            Files.readAllBytes(file)
                    );
                } catch (IOException ex) {
                    throw new UncheckedIOException("Failed to upload " + file, ex);
                }
            }).count();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to walk CORPUS_PATH " + root, ex);
        }
    }

    private String objectName(String relative) {
        String prefix = settings.gcsPrefix();
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix + relative;
    }
}
