package com.acme.assistant.rag;

import java.util.Locale;
import java.util.Set;

public enum MediaKind {
    TEXT,
    IMAGE,
    UNSUPPORTED;

    private static final Set<String> IMAGE_EXT = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "tif", "tiff"
    );

    public static MediaKind of(String filename, String contentType) {
        String mime = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (mime.startsWith("image/")) {
            return IMAGE;
        }
        String ext = extension(filename);
        if (IMAGE_EXT.contains(ext)) {
            return IMAGE;
        }
        if (ext.isBlank() && mime.isBlank()) {
            return UNSUPPORTED;
        }
        return TEXT;
    }

    public static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = slash >= 0 ? filename.substring(slash + 1) : filename;
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
