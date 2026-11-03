package com.acme.assistant.rag.source;

import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Source;
import com.acme.assistant.rag.Fingerprints;
import com.acme.assistant.rag.MediaKind;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class FileSystemKnowledgeSource implements KnowledgeSource {

    private final Source spec;
    private final Path root;

    public FileSystemKnowledgeSource(Source spec) {
        this.spec = spec;
        this.root = Path.of(spec.path()).toAbsolutePath().normalize();
    }

    @Override
    public String id() {
        return spec.id();
    }

    @Override
    public boolean owns(String uri) {
        return uri != null && uri.startsWith("file:");
    }

    @Override
    public List<BlobObject> list() {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            List<BlobObject> objects = new ArrayList<>();
            walk.filter(Files::isRegularFile).forEach(path -> {
                String relative = root.relativize(path).toString().replace('\\', '/');
                if (!Glob.matches(relative, spec.globOrAll())) {
                    return;
                }
                try {
                    byte[] bytes = Files.readAllBytes(path);
                    objects.add(new BlobObject(
                            spec.id(),
                            path.toUri().toString(),
                            relative,
                            Files.probeContentType(path),
                            Fingerprints.sha256(bytes),
                            MediaKind.of(relative, Files.probeContentType(path))
                    ));
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
            return List.copyOf(objects);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to walk " + root, ex);
        }
    }

    @Override
    public Optional<BlobPayload> load(String uri) {
        Path path = Path.of(java.net.URI.create(uri));
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            String relative = root.relativize(path).toString().replace('\\', '/');
            String contentType = Files.probeContentType(path);
            BlobObject object = new BlobObject(
                    spec.id(),
                    uri,
                    relative,
                    contentType,
                    Fingerprints.sha256(bytes),
                    MediaKind.of(relative, contentType)
            );
            return Optional.of(new BlobPayload(object, bytes));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
