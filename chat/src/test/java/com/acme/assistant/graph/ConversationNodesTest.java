package com.acme.assistant.graph;

import com.acme.assistant.agent.IntentRouterAgent;
import com.acme.assistant.domain.AgentIntent;
import com.acme.assistant.rag.KnowledgeRetrievalService;
import com.acme.assistant.rag.RetrievedContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationNodesTest {

    @Test
    void routeWritesIntentName() {
        IntentRouterAgent router = mock(IntentRouterAgent.class);
        KnowledgeRetrievalService retrieval = mock(KnowledgeRetrievalService.class);
        when(router.route("autonomie ?")).thenReturn(AgentIntent.TECHNICAL);

        ConversationNodes nodes = new ConversationNodes(router, retrieval);
        Map<String, Object> update = nodes.route(new ConversationGraphState(Map.of(
                ConversationGraphState.CONVERSATION_ID, "c1",
                ConversationGraphState.USER_MESSAGE, "autonomie ?"
        )));

        assertThat(update).containsEntry(ConversationGraphState.INTENT, "TECHNICAL");
    }

    @Test
    void retrieveStoresContextAndImageUris() {
        IntentRouterAgent router = mock(IntentRouterAgent.class);
        KnowledgeRetrievalService retrieval = mock(KnowledgeRetrievalService.class);
        when(retrieval.retrieve("batterie")).thenReturn(new RetrievedContext(
                "60 kWh",
                List.of(new RetrievedContext.RetrievedImage("gs://kb/dash.png", "image/png", "tableau", new byte[]{1})),
                7
        ));

        ConversationNodes nodes = new ConversationNodes(router, retrieval);
        Map<String, Object> update = nodes.retrieve(new ConversationGraphState(Map.of(
                ConversationGraphState.CONVERSATION_ID, "c1",
                ConversationGraphState.USER_MESSAGE, "batterie"
        )));

        assertThat(update)
                .containsEntry(ConversationGraphState.RAG_CONTEXT, "60 kWh")
                .containsEntry(ConversationGraphState.RAG_IMAGE_URIS, "gs://kb/dash.png")
                .containsEntry(ConversationGraphState.CORPUS_GENERATION, "7");
        verify(retrieval).retrieve("batterie");
    }
}
