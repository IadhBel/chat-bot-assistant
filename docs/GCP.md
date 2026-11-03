# Google Cloud — chat + indexer + MCP on Vertex RAG Engine

Placeholders: `PROJECT_ID`, `REGION` (`europe-west1`), `PROJECT_NUMBER`.

GCP owns embeddings, chunking, parsing and the LLM. This repo keeps the MCP
protocol, the CI trigger, and LangGraph orchestration. Two corpora stay
separate: **docs** (this project) vs **code** (code-rag). Do not fuse them.

```
GitLab/CI ──WIF──► Cloud Run Job indexer
                      │
                      ▼
                 GCS bucket  ──importRagFiles──►  Vertex RAG Engine
                                                      │ retrieve
                      ┌───────────────────────────────┴───────────────┐
                      ▼                                               ▼
              Cloud Run chat                                   Cloud Run MCP
              LangGraph + Gemini                               STREAMABLE /mcp
```

## 1. APIs

```bash
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  aiplatform.googleapis.com \
  documentai.googleapis.com \
  storage.googleapis.com \
  secretmanager.googleapis.com \
  cloudbuild.googleapis.com \
  --project PROJECT_ID
```

## 2. Artifact Registry

```bash
gcloud artifacts repositories create acme \
  --repository-format=docker \
  --location=REGION
```

## 3. Knowledge bucket and RAG corpus

```bash
gcloud storage buckets create gs://PROJECT_ID-acme-knowledge \
  --location=REGION \
  --uniform-bucket-level-access
```

Create a RAG corpus in Vertex AI (console or REST). Note the corpus id and
export it as `RAG_CORPUS`. The indexer writes objects under `knowledge/` and
calls `importRagFiles` on `gs://PROJECT_ID-acme-knowledge/knowledge/`.
Chat and MCP call `retrieveContexts` on the same corpus.

Optional Document AI Layout Parser: create a processor and set
`DOCUMENT_AI_PROCESSOR` to its resource name. Leave empty to let RAG Engine
parse with its native parser.

## 4. Secrets

```bash
gcloud secrets create acme-assistant-api-token --replication-policy=automatic
gcloud secrets create acme-assistant-mcp-token --replication-policy=automatic

printf '%s' "$(openssl rand -base64 32)" | gcloud secrets versions add acme-assistant-api-token --data-file=-
printf '%s' "$(openssl rand -base64 32)" | gcloud secrets versions add acme-assistant-mcp-token --data-file=-
```

## 5. Service accounts

Three identities, one job vs two services:

| Account | Deployable | Roles |
|---|---|---|
| `acme-assistant@` | chat Cloud Run | `aiplatform.user`, `storage.objectViewer`, `secretmanager.secretAccessor` |
| `acme-assistant-mcp@` | MCP Cloud Run | `aiplatform.user`, `storage.objectViewer`, `secretmanager.secretAccessor` |
| `acme-assistant-indexer@` | indexer Job | `aiplatform.user`, `storage.objectAdmin` |
| `acme-assistant-trigger@` | CI / WIF | `run.invoker` on the **job only** |

The RAG Engine service agent
(`service-PROJECT_NUMBER@gcp-sa-aiplatform-re.iam.gserviceaccount.com`) needs
read on the knowledge bucket (`roles/storage.objectViewer`).

Grant the trigger SA `run.invoker` on the job — never `aiplatform.user`. The
pipeline must not hold Vertex or GCS credentials.

## 6. Deploy

Cloud Build builds three images, deploys chat + MCP, and updates the job:

```bash
gcloud builds submit --config cloudbuild.yaml \
  --substitutions=_REGION=REGION,_AR_REPO=acme
```

Then set env on each revision.

### Chat (`acme-assistant`)

```bash
gcloud run services update acme-assistant \
  --region REGION \
  --service-account acme-assistant@PROJECT_ID.iam.gserviceaccount.com \
  --set-env-vars SPRING_PROFILES_ACTIVE=gcp,GCP_PROJECT=PROJECT_ID,GCP_LOCATION=REGION,RAG_BACKEND=vertex,RAG_CORPUS=CORPUS_ID,GCS_BUCKET=PROJECT_ID-acme-knowledge,GCS_PREFIX=knowledge/,GCS_ENABLED=true,SECURITY_ENABLED=true \
  --set-secrets API_TOKEN=acme-assistant-api-token:latest \
  --cpu 2 --memory 2Gi --min-instances 0 --max-instances 4 \
  --timeout 120 --no-cpu-throttling
```

### MCP (`acme-assistant-mcp`)

`--no-allow-unauthenticated` is set in Cloud Build. Keep it. Min instances 1
so the first MCP handshake is not a cold start.

```bash
gcloud run services update acme-assistant-mcp \
  --region REGION \
  --service-account acme-assistant-mcp@PROJECT_ID.iam.gserviceaccount.com \
  --set-env-vars SPRING_PROFILES_ACTIVE=gcp,GCP_PROJECT=PROJECT_ID,GCP_LOCATION=REGION,ACME_MCP_PROVIDER=vertex,RAG_CORPUS=CORPUS_ID,GCS_BUCKET=PROJECT_ID-acme-knowledge,SECURITY_ENABLED=true \
  --set-secrets MCP_READ_TOKEN=acme-assistant-mcp-token:latest \
  --cpu 1 --memory 1Gi --min-instances 1 --max-instances 4 \
  --timeout 60 --no-allow-unauthenticated
```

MCP protocol is **STREAMABLE** at `/mcp` (not stdio, not SSE). Clients send
`Authorization: Bearer $MCP_READ_TOKEN`.

### Indexer job (`acme-assistant-indexer`)

```bash
gcloud run jobs update acme-assistant-indexer \
  --region REGION \
  --service-account acme-assistant-indexer@PROJECT_ID.iam.gserviceaccount.com \
  --set-env-vars GCP_PROJECT=PROJECT_ID,GCP_LOCATION=REGION,ACME_INDEXER_PROVIDER=vertex,RAG_CORPUS=CORPUS_ID,GCS_BUCKET=PROJECT_ID-acme-knowledge,GCS_PREFIX=knowledge/,UPLOAD_CLASSPATH_SEED=true \
  --cpu 2 --memory 2Gi --task-timeout 30m --max-retries 1
```

Trigger from CI (WIF → trigger SA → `run.invoker`):

```bash
gcloud run jobs execute acme-assistant-indexer --region REGION --wait
```

Regional Vertex endpoint is required on Cloud Run:
`{REGION}-aiplatform.googleapis.com:443`. The Java clients set it explicitly.

## 7. What not to build here

On the `gcp` profile, chat does **not** use Cloud SQL / pgvector as the source
of truth. Vertex RAG Engine owns chunk, embed and index. The local profile
still uses Tika + pgvector so laptop work stays offline.

S3 / Azure remain optional extra sources for the local backend only.
