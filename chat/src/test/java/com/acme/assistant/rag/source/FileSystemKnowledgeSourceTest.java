package com.acme.assistant.rag.source;

import com.acme.assistant.infrastructure.config.AcmeProperties.Rag.Source;
import com.acme.assistant.rag.MediaKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemKnowledgeSourceTest {

    @TempDir
    Path tempDir;

    @Test
    void listsPdfHtmlAndImages() throws Exception {
        Files.writeString(tempDir.resolve("notice.pdf"), "%PDF-fake", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("fiche.html"), "<h1>R5</h1>", StandardCharsets.UTF_8);
        Files.write(tempDir.resolve("schema.png"), new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        Source spec = new Source(
                "drop",
                Source.Type.filesystem,
                true,
                null, null, null, "", "**/*",
                tempDir.toString(),
                null, null
        );
        FileSystemKnowledgeSource source = new FileSystemKnowledgeSource(spec);
        List<BlobObject> objects = source.list();

        assertThat(objects).extracting(BlobObject::kind)
                .contains(MediaKind.TEXT, MediaKind.IMAGE);
        assertThat(source.load(objects.getFirst().uri())).isPresent();
    }
}
