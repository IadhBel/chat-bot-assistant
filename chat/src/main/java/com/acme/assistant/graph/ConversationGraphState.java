package com.acme.assistant.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ConversationGraphState extends AgentState {

    public static final String CONVERSATION_ID = "conversationId";
    public static final String USER_MESSAGE = "userMessage";
    public static final String INTENT = "intent";
    public static final String RAG_CONTEXT = "ragContext";
    public static final String RAG_IMAGE_URIS = "ragImageUris";
    public static final String CORPUS_GENERATION = "corpusGeneration";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            CONVERSATION_ID, Channels.base((oldValue, newValue) -> newValue),
            USER_MESSAGE, Channels.base((oldValue, newValue) -> newValue),
            INTENT, Channels.base((oldValue, newValue) -> newValue),
            RAG_CONTEXT, Channels.base((oldValue, newValue) -> newValue),
            RAG_IMAGE_URIS, Channels.base((oldValue, newValue) -> newValue),
            CORPUS_GENERATION, Channels.base((oldValue, newValue) -> newValue)
    );

    public ConversationGraphState(Map<String, Object> initData) {
        super(initData);
    }

    public String conversationId() {
        return this.<String>value(CONVERSATION_ID).orElseThrow();
    }

    public String userMessage() {
        return this.<String>value(USER_MESSAGE).orElseThrow();
    }

    public String intent() {
        return this.<String>value(INTENT).orElse("CONCIERGE");
    }

    public String ragContext() {
        return this.<String>value(RAG_CONTEXT).orElse("");
    }

    public List<String> ragImageUris() {
        String raw = this.<String>value(RAG_IMAGE_URIS).orElse("");
        if (raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("\n")).filter(uri -> !uri.isBlank()).toList();
    }

    public long corpusGeneration() {
        String raw = this.<String>value(CORPUS_GENERATION).orElse("0");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
