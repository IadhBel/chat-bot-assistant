package com.acme.assistant.infrastructure.memory;

import com.acme.assistant.infrastructure.config.AcmeProperties;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfiguration {

    @Bean
    ChatMemoryProvider chatMemoryProvider(AcmeProperties properties) {
        int maxMessages = properties.memory().maxMessages();
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .build();
    }
}
