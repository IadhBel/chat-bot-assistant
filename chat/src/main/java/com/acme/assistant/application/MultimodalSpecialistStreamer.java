package com.acme.assistant.application;

import com.acme.assistant.rag.RetrievedContext;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class MultimodalSpecialistStreamer {

    private static final String TECHNICAL_SYSTEM = """
            Tu es l'expert technique Acme. Tu t'appuies sur le contexte et les
            images. Pas d'instruction de réparation haute tension au client.
            """;

    private static final String AFTER_SALES_SYSTEM = """
            Tu es le conseiller après-vente Acme. Tu t'appuies sur le contexte
            et les images. Tu n'affirmes un rappel que si le VIN le confirme.
            """;

    private final StreamingChatModel pro;
    private final StreamingChatModel flash;

    public MultimodalSpecialistStreamer(
            @Qualifier("geminiProStreamingChatModel") StreamingChatModel pro,
            @Qualifier("geminiFlashStreamingChatModel") StreamingChatModel flash
    ) {
        this.pro = pro;
        this.flash = flash;
    }

    public Flux<String> technical(String context, String message, List<RetrievedContext.RetrievedImage> images) {
        return stream(pro, TECHNICAL_SYSTEM, context, message, images);
    }

    public Flux<String> afterSales(String context, String message, List<RetrievedContext.RetrievedImage> images) {
        return stream(flash, AFTER_SALES_SYSTEM, context, message, images);
    }

    private static Flux<String> stream(
            StreamingChatModel model,
            String system,
            String context,
            String message,
            List<RetrievedContext.RetrievedImage> images
    ) {
        List<dev.langchain4j.data.message.Content> parts = new ArrayList<>();
        parts.add(TextContent.from("Contexte documentaire:\n" + context + "\n\nQuestion client:\n" + message));
        for (RetrievedContext.RetrievedImage image : images) {
            String mime = image.mimeType() == null ? "image/png" : image.mimeType();
            parts.add(ImageContent.from(Base64.getEncoder().encodeToString(image.bytes()), mime));
        }
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(SystemMessage.from(system), UserMessage.from(parts)))
                .build();
        return Flux.create(sink -> model.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.next(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                sink.complete();
            }

            @Override
            public void onError(Throwable error) {
                sink.error(error);
            }
        }));
    }
}
