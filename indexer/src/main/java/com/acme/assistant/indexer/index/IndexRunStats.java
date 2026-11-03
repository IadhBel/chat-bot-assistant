package com.acme.assistant.indexer.index;

public record IndexRunStats(int uploaded, long imported, long failed, long skipped, long corpusGeneration) {
}
