package com.acme.assistant.rag.parse;

import com.acme.assistant.infrastructure.config.AcmeProperties;
import com.acme.assistant.infrastructure.config.AcmeProperties.Llm.Provider;
import com.acme.assistant.rag.source.BlobPayload;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "acme.rag.backend", havingValue = "local", matchIfMissing = true)
public class ImageCaptioner {

    private final ChatModel flash;
    private final boolean stub;

    public ImageCaptioner(
            @Qualifier("geminiFlashChatModel") ChatModel flash,
            AcmeProperties properties
    ) {
        this.flash = flash;
        this.stub = properties.llm().provider() == Provider.stub;
    }

    public String caption(BlobPayload payload) {
        String filename = payload.object().path();
        if (stub) {
            return "Image Acme (" + filename + "). Décrire le visuel une fois Vertex activé.";
        }
        String mime = mimeType(payload);
        String base64 = Base64.getEncoder().encodeToString(payload.bytes());
        String reply = flash.chat(UserMessage.from(
                TextContent.from("""
                        Tu es un expert produit Acme. Décris cette image pour un RAG:
                        modèle visible, organe, tableau de bord, pièce, défaut éventuel.
                        Texte court, factuel, en français. Fichier: %s.
                        """.formatted(filename)),
                ImageContent.from(base64, mime)
        )).aiMessage().text();
        return reply == null ? "Image Acme (" + filename + ")." : reply;
    }

    public static String mimeType(BlobPayload payload) {
        if (payload.object().contentType() != null && payload.object().contentType().startsWith("image/")) {
            return payload.object().contentType();
        }
        String ext = filenameExt(payload.object().path());
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "tif", "tiff" -> "image/tiff";
            default -> "image/png";
        };
    }

    private static String filenameExt(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
