package com.acme.assistant.corpus;

public record ImportResult(long imported, long failed, long skipped, long corpusGeneration) {
}
