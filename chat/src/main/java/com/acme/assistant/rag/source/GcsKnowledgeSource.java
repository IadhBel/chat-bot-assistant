package com.acme.assistant.rag.source;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Source;
import com.acme.assistant.rag.MediaKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GcsKnowledgeSource implements KnowledgeSource {

    private final Source spec;
    private final Storage storage;

    public GcsKnowledgeSource(Source spec) {
        this.spec = spec;
        StorageOptions.Builder builder = StorageOptions.newBuilder();
        if (spec.project() != null && !spec.project().isBlank()) {
            builder.setProjectId(spec.project());
        }
        this.storage = builder.build().getService();
    }

    @Override
    public String id() {
        return spec.id();
    }

    @Override
    public boolean owns(String uri) {
        return uri != null && uri.startsWith("gs://" + spec.bucket() + "/");
    }

    @Override
    public List<BlobObject> list() {
        List<BlobObject> objects = new ArrayList<>();
        for (Blob blob : storage.list(spec.bucket(), Storage.BlobListOption.prefix(spec.prefixOrEmpty())).iterateAll()) {
            if (blob.isDirectory() || blob.getSize() == null || blob.getSize() == 0) {
                continue;
            }
            if (!Glob.matches(blob.getName(), spec.globOrAll())) {
                continue;
            }
            objects.add(toObject(blob));
        }
        return List.copyOf(objects);
    }

    @Override
    public Optional<BlobPayload> load(String uri) {
        String key = uri.substring(("gs://" + spec.bucket() + "/").length());
        Blob blob = storage.get(spec.bucket(), key);
        if (blob == null || !blob.exists()) {
            return Optional.empty();
        }
        return Optional.of(new BlobPayload(toObject(blob), blob.getContent()));
    }

    private BlobObject toObject(Blob blob) {
        String fingerprint = blob.getEtag() != null ? blob.getEtag() : String.valueOf(blob.getGeneration());
        return new BlobObject(
                spec.id(),
                "gs://" + spec.bucket() + "/" + blob.getName(),
                blob.getName(),
                blob.getContentType(),
                fingerprint,
                MediaKind.of(blob.getName(), blob.getContentType())
        );
    }
}
