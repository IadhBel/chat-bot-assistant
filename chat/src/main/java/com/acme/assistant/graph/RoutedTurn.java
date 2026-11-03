package com.acme.assistant.graph;

import com.acme.assistant.domain.AgentIntent;
import com.acme.assistant.rag.RetrievedContext;

import java.util.List;

public record RoutedTurn(
        String conversationId,
        String userMessage,
        AgentIntent intent,
        String ragContext,
        List<RetrievedContext.RetrievedImage> images,
        long corpusGeneration
) {
}
