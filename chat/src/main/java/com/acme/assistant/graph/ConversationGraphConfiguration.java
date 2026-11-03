package com.acme.assistant.graph;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Configuration
public class ConversationGraphConfiguration {

    public static final String ROUTE = "route";
    public static final String RETRIEVE = "retrieve";
    public static final String SKIP_RETRIEVE = "skipRetrieve";

    @Bean
    CompiledGraph<ConversationGraphState> conversationGraph(ConversationNodes nodes) throws GraphStateException {
        return new StateGraph<>(ConversationGraphState.SCHEMA, ConversationGraphState::new)
                .addNode(ROUTE, node_async(nodes::route))
                .addNode(RETRIEVE, node_async(nodes::retrieve))
                .addNode(SKIP_RETRIEVE, node_async(nodes::skipRetrieve))
                .addEdge(START, ROUTE)
                .addConditionalEdges(
                        ROUTE,
                        edge_async(state -> needsRag(state.intent()) ? RETRIEVE : SKIP_RETRIEVE),
                        java.util.Map.of(
                                RETRIEVE, RETRIEVE,
                                SKIP_RETRIEVE, SKIP_RETRIEVE
                        )
                )
                .addEdge(RETRIEVE, END)
                .addEdge(SKIP_RETRIEVE, END)
                .compile();
    }

    static boolean needsRag(String intent) {
        return "TECHNICAL".equals(intent) || "AFTER_SALES".equals(intent);
    }
}
