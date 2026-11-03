package com.acme.assistant.rag.source;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Source;
import com.acme.assistant.rag.MediaKind;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AzureBlobKnowledgeSource implements KnowledgeSource {

    private final Source spec;
    private final BlobContainerClient container;

    public AzureBlobKnowledgeSource(Source spec) {
        this.spec = spec;
        this.container = new BlobServiceClientBuilder()
                .connectionString(spec.connectionString())
                .buildClient()
                .getBlobContainerClient(spec.container());
    }

    @Override
    public String id() {
        return spec.id();
    }

    @Override
    public boolean owns(String uri) {
        return uri != null && uri.startsWith("azure://" + spec.container() + "/");
    }

    @Override
    public List<BlobObject> list() {
        List<BlobObject> objects = new ArrayList<>();
        for (BlobItem item : container.listBlobs()) {
            if (Boolean.TRUE.equals(item.isPrefix())) {
                continue;
            }
            if (!item.getName().startsWith(spec.prefixOrEmpty())) {
                continue;
            }
            if (!Glob.matches(item.getName(), spec.globOrAll())) {
                continue;
            }
            objects.add(toObject(item));
        }
        return List.copyOf(objects);
    }

    @Override
    public Optional<BlobPayload> load(String uri) {
        String key = uri.substring(("azure://" + spec.container() + "/").length());
        BlobClient client = container.getBlobClient(key);
        if (!client.exists()) {
            return Optional.empty();
        }
        BlobProperties properties = client.getProperties();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        client.downloadStream(buffer);
        BlobObject object = new BlobObject(
                spec.id(),
                uri,
                key,
                properties.getContentType(),
                properties.getETag(),
                MediaKind.of(key, properties.getContentType())
        );
        return Optional.of(new BlobPayload(object, buffer.toByteArray()));
    }

    private BlobObject toObject(BlobItem item) {
        String etag = item.getProperties() == null ? item.getName() : item.getProperties().getETag();
        String contentType = item.getProperties() == null ? null : item.getProperties().getContentType();
        return new BlobObject(
                spec.id(),
                "azure://" + spec.container() + "/" + item.getName(),
                item.getName(),
                contentType,
                etag,
                MediaKind.of(item.getName(), contentType)
        );
    }
}
