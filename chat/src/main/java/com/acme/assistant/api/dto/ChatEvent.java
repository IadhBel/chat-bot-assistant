package com.acme.assistant.api.dto;

public record ChatEvent(String type, String text, String intent, String model, Long corpusGeneration) {

    public static ChatEvent token(String text) {
        return new ChatEvent("token", text, null, null, null);
    }

    public static ChatEvent meta(String intent, String model) {
        return new ChatEvent("meta", null, intent, model, null);
    }

    public static ChatEvent meta(String intent, String model, long corpusGeneration) {
        return new ChatEvent("meta", null, intent, model, corpusGeneration);
    }

    public static ChatEvent done() {
        return new ChatEvent("done", null, null, null, null);
    }

    public static ChatEvent error(String text) {
        return new ChatEvent("error", text, null, null, null);
    }
}
