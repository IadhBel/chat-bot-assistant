package com.acme.assistant.graph;

import com.acme.assistant.agent.IntentRouterAgent;
import com.acme.assistant.domain.AgentIntent;
import com.acme.assistant.rag.KnowledgeRetrievalService;
import com.acme.assistant.rag.RetrievedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ConversationNodes {

    private static final Logger log = LoggerFactory.getLogger(ConversationNodes.class);

    private final IntentRouterAgent router;
    private final KnowledgeRetrievalService retrievalService;

    public ConversationNodes(IntentRouterAgent router, KnowledgeRetrievalService retrievalService) {
        this.router = router;
        this.retrievalService = retrievalService;
    }

    public Map<String, Object> route(ConversationGraphState state) {
        AgentIntent intent = router.route(state.userMessage());
        log.info("Routed conversation {} to {}", state.conversationId(), intent);
        return Map.of(ConversationGraphState.INTENT, intent.name());
    }

    public Map<String, Object> retrieve(ConversationGraphState state) {
        RetrievedContext context = retrievalService.retrieve(state.userMessage());
        log.debug(
                "RAG context size={} images={} generation={} for conversation {}",
                context.text().length(),
                context.images().size(),
                context.corpusGeneration(),
                state.conversationId()
        );
        return toState(context);
    }

    public Map<String, Object> skipRetrieve(ConversationGraphState state) {
        return Map.of(
                ConversationGraphState.RAG_CONTEXT, "",
                ConversationGraphState.RAG_IMAGE_URIS, "",
                ConversationGraphState.CORPUS_GENERATION, "0"
        );
    }

    static Map<String, Object> toState(RetrievedContext context) {
        String uris = context.images().stream()
                .map(RetrievedContext.RetrievedImage::uri)
                .collect(Collectors.joining("\n"));
        return Map.of(
                ConversationGraphState.RAG_CONTEXT, context.text(),
                ConversationGraphState.RAG_IMAGE_URIS, uris,
                ConversationGraphState.CORPUS_GENERATION, String.valueOf(context.corpusGeneration())
        );
    }
}
