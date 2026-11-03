package com.acme.assistant.corpus;

import com.google.cloud.aiplatform.v1.GcsSource;
import com.google.cloud.aiplatform.v1.ImportRagFilesConfig;
import com.google.cloud.aiplatform.v1.ImportRagFilesResponse;
import com.google.cloud.aiplatform.v1.RagContexts;
import com.google.cloud.aiplatform.v1.RagFile;
import com.google.cloud.aiplatform.v1.RagFileChunkingConfig;
import com.google.cloud.aiplatform.v1.RagFileParsingConfig;
import com.google.cloud.aiplatform.v1.RagFileTransformationConfig;
import com.google.cloud.aiplatform.v1.RagQuery;
import com.google.cloud.aiplatform.v1.RagRetrievalConfig;
import com.google.cloud.aiplatform.v1.RetrieveContextsRequest;
import com.google.cloud.aiplatform.v1.RetrieveContextsResponse;
import com.google.cloud.aiplatform.v1.VertexRagDataServiceClient;
import com.google.cloud.aiplatform.v1.VertexRagDataServiceSettings;
import com.google.cloud.aiplatform.v1.VertexRagServiceClient;
import com.google.cloud.aiplatform.v1.VertexRagServiceSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Vertex RAG Engine over the regional AI Platform endpoint.
 *
 * <p>The regional endpoint is required on Cloud Run: the global
 * {@code aiplatform.googleapis.com} answers UNIMPLEMENTED for retrieve/import.
 */
public final class VertexRagEngineClient implements VertexRagPort, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(VertexRagEngineClient.class);
    private static final long IMPORT_TIMEOUT_MINUTES = 30;

    private final VertexRagSettings settings;
    private final GcsCorpusGenerationStore generationStore;
    private final VertexRagServiceClient retrieveClient;
    private final VertexRagDataServiceClient dataClient;

    public VertexRagEngineClient(VertexRagSettings settings, GcsCorpusGenerationStore generationStore) {
        this.settings = settings;
        this.generationStore = generationStore;
        try {
            String endpoint = settings.regionalEndpoint();
            this.retrieveClient = VertexRagServiceClient.create(
                    VertexRagServiceSettings.newBuilder().setEndpoint(endpoint).build());
            this.dataClient = VertexRagDataServiceClient.create(
                    VertexRagDataServiceSettings.newBuilder().setEndpoint(endpoint).build());
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to create Vertex RAG clients", ex);
        }
    }

    @Override
    public List<RagHit> retrieve(String query, int topK) {
        int limit = topK > 0 ? topK : settings.topK();
        RetrieveContextsRequest request = RetrieveContextsRequest.newBuilder()
                .setParent(settings.locationParent())
                .setQuery(RagQuery.newBuilder()
                        .setText(query == null ? "" : query)
                        .setRagRetrievalConfig(RagRetrievalConfig.newBuilder().setTopK(limit).build())
                        .build())
                .setVertexRagStore(RetrieveContextsRequest.VertexRagStore.newBuilder()
                        .addRagResources(RetrieveContextsRequest.VertexRagStore.RagResource.newBuilder()
                                .setRagCorpus(settings.corpusName())
                                .build())
                        .build())
                .build();
        RetrieveContextsResponse response = retrieveClient.retrieveContexts(request);
        List<RagHit> hits = new ArrayList<>();
        for (RagContexts.Context context : response.getContexts().getContextsList()) {
            hits.add(new RagHit(
                    context.getSourceUri(),
                    context.getSourceDisplayName(),
                    context.getText(),
                    context.hasScore() ? context.getScore() : 0.0
            ));
        }
        return List.copyOf(hits);
    }

    @Override
    public ImportResult importGcsPrefix(String gcsUri) {
        String uri = (gcsUri == null || gcsUri.isBlank()) ? settings.importGcsUri() : gcsUri;
        ImportRagFilesConfig.Builder config = ImportRagFilesConfig.newBuilder()
                .setGcsSource(GcsSource.newBuilder().addUris(uri).build())
                .setRagFileTransformationConfig(RagFileTransformationConfig.newBuilder()
                        .setRagFileChunkingConfig(RagFileChunkingConfig.newBuilder()
                                .setFixedLengthChunking(RagFileChunkingConfig.FixedLengthChunking.newBuilder()
                                        .setChunkSize(settings.chunkSize())
                                        .setChunkOverlap(settings.chunkOverlap())
                                        .build())
                                .build())
                        .build());
        if (settings.layoutParserEnabled()) {
            config.setRagFileParsingConfig(RagFileParsingConfig.newBuilder()
                    .setLayoutParser(RagFileParsingConfig.LayoutParser.newBuilder()
                            .setProcessorName(settings.layoutParserProcessor())
                            .build())
                    .build());
        }
        try {
            ImportRagFilesResponse response = dataClient
                    .importRagFilesAsync(settings.corpusName(), config.build())
                    .get(IMPORT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            long generation = generationStore.increment();
            log.info(
                    "Vertex RAG import uri={} imported={} failed={} skipped={} generation={}",
                    uri,
                    response.getImportedRagFilesCount(),
                    response.getFailedRagFilesCount(),
                    response.getSkippedRagFilesCount(),
                    generation
            );
            return new ImportResult(
                    response.getImportedRagFilesCount(),
                    response.getFailedRagFilesCount(),
                    response.getSkippedRagFilesCount(),
                    generation
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Vertex RAG import interrupted for " + uri, ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Vertex RAG import failed for " + uri, ex);
        }
    }

    @Override
    public long corpusGeneration() {
        return generationStore.read();
    }

    @Override
    public List<RagFileInfo> listFiles() {
        List<RagFileInfo> files = new ArrayList<>();
        for (RagFile file : dataClient.listRagFiles(settings.corpusName()).iterateAll()) {
            String sourceUri = file.hasGcsSource() && file.getGcsSource().getUrisCount() > 0
                    ? file.getGcsSource().getUris(0)
                    : "";
            files.add(new RagFileInfo(file.getName(), file.getDisplayName(), sourceUri));
        }
        return List.copyOf(files);
    }

    @Override
    public void close() {
        retrieveClient.close();
        dataClient.close();
    }
}
