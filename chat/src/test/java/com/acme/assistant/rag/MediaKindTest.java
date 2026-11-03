package com.acme.assistant.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaKindTest {

    @Test
    void classifiesImagesAndOfficeDocuments() {
        assertThat(MediaKind.of("schema.png", null)).isEqualTo(MediaKind.IMAGE);
        assertThat(MediaKind.of("photo.JPEG", "application/octet-stream")).isEqualTo(MediaKind.IMAGE);
        assertThat(MediaKind.of("notice.pdf", "application/pdf")).isEqualTo(MediaKind.TEXT);
        assertThat(MediaKind.of("garantie.docx", null)).isEqualTo(MediaKind.TEXT);
        assertThat(MediaKind.of("fiche.html", "text/html")).isEqualTo(MediaKind.TEXT);
        assertThat(MediaKind.of("plan.webp", "image/webp")).isEqualTo(MediaKind.IMAGE);
    }
}
