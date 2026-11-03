package com.acme.assistant.indexer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Cloud Run Job entry point: publish knowledge to GCS and import into Vertex
 * RAG Engine, then exit. The pipeline only holds {@code run.invoker}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class IndexerApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(IndexerApplication.class, args)));
    }
}
