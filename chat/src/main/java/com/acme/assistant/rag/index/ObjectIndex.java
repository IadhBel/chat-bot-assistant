package com.acme.assistant.rag.index;

import java.util.Optional;
import java.util.Set;

public interface ObjectIndex {

    Optional<String> fingerprint(String uri);

    void put(IndexedObject object);

    void remove(String uri);

    Set<String> uris();
}
