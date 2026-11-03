package com.acme.assistant.rag.index;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MemoryObjectIndex implements ObjectIndex {

    private final Map<String, IndexedObject> objects = new ConcurrentHashMap<>();

    @Override
    public Optional<String> fingerprint(String uri) {
        return Optional.ofNullable(objects.get(uri)).map(IndexedObject::fingerprint);
    }

    @Override
    public void put(IndexedObject object) {
        objects.put(object.uri(), object);
    }

    @Override
    public void remove(String uri) {
        objects.remove(uri);
    }

    @Override
    public Set<String> uris() {
        return Set.copyOf(objects.keySet());
    }
}
