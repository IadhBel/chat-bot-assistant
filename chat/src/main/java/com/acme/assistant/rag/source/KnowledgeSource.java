package com.acme.assistant.rag.source;

import java.util.List;
import java.util.Optional;

public interface KnowledgeSource {

    String id();

    boolean owns(String uri);

    List<BlobObject> list();

    Optional<BlobPayload> load(String uri);
}
