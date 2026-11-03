package com.acme.assistant.rag.source;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;

final class Glob {

    private Glob() {
    }

    static boolean matches(String relativePath, String glob) {
        if (glob == null || glob.isBlank() || "**/*".equals(glob) || "*".equals(glob)) {
            return true;
        }
        String normalized = relativePath.replace('\\', '/');
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        return matcher.matches(java.nio.file.Path.of(normalized))
                || matcher.matches(java.nio.file.Path.of(normalized).getFileName());
    }
}
