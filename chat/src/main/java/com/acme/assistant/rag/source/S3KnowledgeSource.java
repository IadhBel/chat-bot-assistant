package com.acme.assistant.rag.source;

import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Source;
import com.acme.assistant.rag.MediaKind;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class S3KnowledgeSource implements KnowledgeSource {

    private final Source spec;
    private final S3Client client;

    public S3KnowledgeSource(Source spec) {
        this.spec = spec;
        this.client = S3Client.builder()
                .region(Region.of(spec.region() == null || spec.region().isBlank() ? "eu-west-1" : spec.region()))
                .build();
    }

    @Override
    public String id() {
        return spec.id();
    }

    @Override
    public boolean owns(String uri) {
        return uri != null && uri.startsWith("s3://" + spec.bucket() + "/");
    }

    @Override
    public List<BlobObject> list() {
        List<BlobObject> objects = new ArrayList<>();
        client.listObjectsV2Paginator(ListObjectsV2Request.builder()
                        .bucket(spec.bucket())
                        .prefix(spec.prefixOrEmpty())
                        .build())
                .contents()
                .forEach(object -> {
                    if (object.key().endsWith("/")) {
                        return;
                    }
                    if (!Glob.matches(object.key(), spec.globOrAll())) {
                        return;
                    }
                    objects.add(toObject(object));
                });
        return List.copyOf(objects);
    }

    @Override
    public Optional<BlobPayload> load(String uri) {
        String key = uri.substring(("s3://" + spec.bucket() + "/").length());
        ResponseBytes<GetObjectResponse> response = client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(spec.bucket())
                .key(key)
                .build());
        GetObjectResponse meta = response.response();
        BlobObject object = new BlobObject(
                spec.id(),
                uri,
                key,
                meta.contentType(),
                meta.eTag(),
                MediaKind.of(key, meta.contentType())
        );
        return Optional.of(new BlobPayload(object, response.asByteArray()));
    }

    private BlobObject toObject(S3Object object) {
        return new BlobObject(
                spec.id(),
                "s3://" + spec.bucket() + "/" + object.key(),
                object.key(),
                null,
                object.eTag(),
                MediaKind.of(object.key(), null)
        );
    }
}
