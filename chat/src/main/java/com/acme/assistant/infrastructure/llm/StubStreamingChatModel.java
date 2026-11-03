package com.acme.assistant.infrastructure.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * Word-by-word streaming counterpart of {@link StubChatModel}, so the WebFlux
 * SSE path can be exercised without Vertex.
 */
public final class StubStreamingChatModel implements StreamingChatModel, AutoCloseable {

    private final String modelName;

    public StubStreamingChatModel(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        String userText = StubChatModel.lastUserText(chatRequest.messages());
        String reply = StubResponses.reply(userText);
        try {
            for (String token : reply.split("(?<=\\s)")) {
                handler.onPartialResponse(token);
            }
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from(reply))
                    .modelName(modelName)
                    .build());
        } catch (RuntimeException ex) {
            handler.onError(ex);
        }
    }

    @Override
    public void close() {
        // no-op
    }
}
