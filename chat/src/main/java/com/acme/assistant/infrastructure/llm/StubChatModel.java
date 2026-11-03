package com.acme.assistant.infrastructure.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;

/**
 * Deterministic stand-in for Vertex. Classification prompts return an
 * {@code AgentIntent} name so {@code @AiService} structured output still works
 * offline. Everything else is a short grounded reply — lexical RAG supplies
 * the facts, this model does not invent product data.
 */
public final class StubChatModel implements ChatModel, AutoCloseable {

    private final String modelName;

    public StubChatModel(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        String userText = lastUserText(chatRequest.messages());
        String reply = StubResponses.reply(userText);
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(reply))
                .modelName(modelName)
                .build();
    }

    static String lastUserText(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage userMessage) {
                return userMessage.contents().stream()
                        .map(content -> content instanceof dev.langchain4j.data.message.TextContent text
                                ? text.text()
                                : "")
                        .filter(text -> !text.isBlank())
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse("");
            }
        }
        return "";
    }

    @Override
    public void close() {
        // no-op: there is no client to shut down
    }
}
