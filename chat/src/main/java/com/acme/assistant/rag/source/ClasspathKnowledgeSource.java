package com.acme.assistant.rag.source;

import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Source;
import com.acme.assistant.rag.Fingerprints;
import com.acme.assistant.rag.MediaKind;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ClasspathKnowledgeSource implements KnowledgeSource {

    private final Source spec;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public ClasspathKnowledgeSource(Source spec) {
        this.spec = spec;
    }

    @Override
    public String id() {
        return spec.id();
    }

    @Override
    public boolean owns(String uri) {
        return uri != null && uri.startsWith("classpath:");
    }

    @Override
    public List<BlobObject> list() {
        try {
            String pattern = "classpath*:" + spec.prefixOrEmpty() + "**";
            Resource[] resources = resolver.getResources(pattern);
            List<BlobObject> objects = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.isReadable() || resource.getFilename() == null) {
                    continue;
                }
                String filename = resource.getFilename();
                if (!Glob.matches(filename, spec.globOrAll())) {
                    continue;
                }
                byte[] bytes = resource.getContentAsByteArray();
                String path = spec.prefixOrEmpty() + filename;
                objects.add(new BlobObject(
                        spec.id(),
                        "classpath:" + path,
                        path,
                        null,
                        Fingerprints.sha256(bytes),
                        MediaKind.of(filename, null)
                ));
            }
            return List.copyOf(objects);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to list classpath knowledge at " + spec.prefixOrEmpty(), ex);
        }
    }

    @Override
    public Optional<BlobPayload> load(String uri) {
        return list().stream()
                .filter(object -> object.uri().equals(uri))
                .map(object -> new BlobPayload(object, read(object.path())))
                .findFirst();
    }

    private byte[] read(String path) {
        try {
            Resource resource = resolver.getResource("classpath:" + path);
            return resource.getContentAsByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
