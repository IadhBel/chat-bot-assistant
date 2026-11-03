package com.acme.assistant.rag.index;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class JdbcObjectIndex implements ObjectIndex {

    private final JdbcTemplate jdbc;

    public JdbcObjectIndex(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> fingerprint(String uri) {
        return jdbc.query(
                "SELECT fingerprint FROM knowledge_object WHERE uri = ?",
                (rs, rowNum) -> rs.getString(1),
                uri
        ).stream().findFirst();
    }

    @Override
    public void put(IndexedObject object) {
        jdbc.update("""
                        INSERT INTO knowledge_object (uri, fingerprint, modality, indexed_at)
                        VALUES (?, ?, ?, now())
                        ON CONFLICT (uri) DO UPDATE
                          SET fingerprint = excluded.fingerprint,
                              modality = excluded.modality,
                              indexed_at = now()
                        """,
                object.uri(),
                object.fingerprint(),
                object.modality()
        );
    }

    @Override
    public void remove(String uri) {
        jdbc.update("DELETE FROM knowledge_object WHERE uri = ?", uri);
    }

    @Override
    public Set<String> uris() {
        return new HashSet<>(jdbc.query("SELECT uri FROM knowledge_object", (rs, rowNum) -> rs.getString(1)));
    }
}
