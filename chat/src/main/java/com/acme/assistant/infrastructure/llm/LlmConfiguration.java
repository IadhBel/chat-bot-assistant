package com.acme.assistant.infrastructure.llm;

import com.acme.assistant.infrastructure.config.AcmeProperties;
import com.acme.assistant.infrastructure.config.AcmeProperties.Llm.Provider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiChatModel;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Two Gemini SKUs, not one autowired {@code ChatModel}: Flash for routing and
 * conversational turns, Pro for technical answers. Bean names are referenced
 * by {@code @AiService(wiringMode = EXPLICIT)}.
 */
@Configuration
public class LlmConfiguration {

    @Bean(name = "geminiFlashChatModel", destroyMethod = "close")
    ChatModel geminiFlashChatModel(AcmeProperties properties) {
        if (properties.llm().provider() == Provider.stub) {
            return new StubChatModel(properties.llm().vertex().flashModel());
        }
        return vertexChat(properties, properties.llm().vertex().flashModel());
    }

    @Bean(name = "geminiFlashStreamingChatModel", destroyMethod = "close")
    StreamingChatModel geminiFlashStreamingChatModel(AcmeProperties properties) {
        if (properties.llm().provider() == Provider.stub) {
            return new StubStreamingChatModel(properties.llm().vertex().flashModel());
        }
        return vertexStreaming(properties, properties.llm().vertex().flashModel());
    }

    @Bean(name = "geminiProChatModel", destroyMethod = "close")
    ChatModel geminiProChatModel(AcmeProperties properties) {
        if (properties.llm().provider() == Provider.stub) {
            return new StubChatModel(properties.llm().vertex().proModel());
        }
        return vertexChat(properties, properties.llm().vertex().proModel());
    }

    @Bean(name = "geminiProStreamingChatModel", destroyMethod = "close")
    StreamingChatModel geminiProStreamingChatModel(AcmeProperties properties) {
        if (properties.llm().provider() == Provider.stub) {
            return new StubStreamingChatModel(properties.llm().vertex().proModel());
        }
        return vertexStreaming(properties, properties.llm().vertex().proModel());
    }

    private static VertexAiGeminiChatModel vertexChat(AcmeProperties properties, String modelName) {
        AcmeProperties.Llm.Vertex vertex = requireVertex(properties);
        return VertexAiGeminiChatModel.builder()
                .project(vertex.project())
                .location(vertex.location())
                .modelName(modelName)
                .temperature((float) vertex.temperature())
                .maxOutputTokens(vertex.maxOutputTokens())
                .maxRetries(vertex.maxRetries())
                .logRequests(vertex.logRequests())
                .logResponses(vertex.logRequests())
                .build();
    }

    private static VertexAiGeminiStreamingChatModel vertexStreaming(AcmeProperties properties, String modelName) {
        AcmeProperties.Llm.Vertex vertex = requireVertex(properties);
        return VertexAiGeminiStreamingChatModel.builder()
                .project(vertex.project())
                .location(vertex.location())
                .modelName(modelName)
                .temperature((float) vertex.temperature())
                .maxOutputTokens(vertex.maxOutputTokens())
                .logRequests(vertex.logRequests())
                .logResponses(vertex.logRequests())
                .build();
    }

    private static AcmeProperties.Llm.Vertex requireVertex(AcmeProperties properties) {
        AcmeProperties.Llm.Vertex vertex = properties.llm().vertex();
        if (vertex.project() == null || vertex.project().isBlank()) {
            throw new IllegalStateException(
                    "GCP_PROJECT is required when acme.llm.provider=vertex");
        }
        return vertex;
    }
}
