# Assistant conversationnel Acme

Trois déploiables Maven sur **un corpus Vertex RAG Engine** :

| Module | Rôle | Runtime |
|---|---|---|
| `chat` | LangGraph4j + agents LangChain4j, SSE | Cloud Run Service (WebFlux) |
| `indexer` | Fichiers → GCS → `importRagFiles` | Cloud Run Job |
| `mcp` | Outils `search_knowledge` / `list_sources` | Cloud Run Service (Web MVC, STREAMABLE `/mcp`) |
| `core` | `VertexRagPort` partagé | jar |

GCP parse, chunk, embed et indexe. Ce repo garde le protocole MCP, le trigger
CI et l’orchestration LangGraph. Le corpus **docs** ici n’est pas le corpus
**code** de code-rag.

## Stack

| Couche | Choix | Version |
|---|---|---|
| Runtime | Java 25, Spring Boot | 4.1.0 |
| Chat | WebFlux, LangChain4j, LangGraph4j | 1.19.0 / 1.8.24 |
| MCP | Spring AI MCP STREAMABLE | 2.0.0 |
| LLM prod | Vertex Gemini Flash + Pro | europe-west1 |
| Index prod | Vertex RAG Engine + GCS | — |
| Local / tests | stub LLM + Tika/pgvector (chat) / in-memory RAG | — |

## Architecture

```
CI (WIF, run.invoker) → indexer Job → GCS → Vertex RAG Engine
                                              │ retrieveContexts
                     ┌────────────────────────┴─────────────────┐
                     ▼                                          ▼
                   chat                                       mcp
        START → route → retrieve → specialist         /mcp  STREAMABLE
```

Le graphe **route et enrichit**. Il ne génère pas la réponse: le streaming
token-par-token reste sur l’agent LangChain4j.

Profil `gcp` (`RAG_BACKEND=vertex`): chat et MCP ne parlent plus à pgvector.
Profil `local`: chat garde Tika + embeddings pour travailler hors GCP.

## Démarrage local

```bash
docker compose up -d postgres
mvn -pl chat spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl mcp spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl indexer spring-boot:run -Dspring-boot.run.profiles=local
```

Ou le stack chat + MCP:

```bash
docker compose up --build
```

UI chat: [http://localhost:8080](http://localhost:8080)

```bash
curl -N -X POST http://localhost:8080/api/v1/conversations/demo/messages \
  -H 'Content-Type: application/json' \
  -d '{"message":"Quelle est l autonomie de la Megane E-Tech ?"}'
```

MCP local (stub, pas d’auth): `http://localhost:8081/mcp`.

## Vertex en local (chat LLM only)

```bash
gcloud auth application-default login
export GCP_PROJECT=...
export ACME_LLM_PROVIDER=vertex
mvn -pl chat spring-boot:run -Dspring-boot.run.profiles=local
```

Pour brancher retrieve sur le corpus Vertex: `RAG_BACKEND=vertex` +
`RAG_CORPUS` + `GCS_BUCKET` (voir [docs/GCP.md](docs/GCP.md)).

## API chat

| Méthode | Chemin | Description |
|---|---|---|
| `POST` | `/api/v1/conversations/{id}/messages` | Chat SSE (`text/event-stream`) |
| `POST` | `/api/v1/knowledge` | Ingestion locale (backend `local` only) |
| `POST` | `/api/v1/knowledge/sync` | Sync storages (backend `local` only) |
| `POST` | `/api/v1/llm/sync` | Aligne Flash + Pro sur `corpusGeneration` |
| `GET` | `/actuator/health` | Liveness / readiness Cloud Run |

Événements SSE: `meta` (intent + modèle + `corpusGeneration`), `token`, `done`, `error`.

## MCP

Outils: `list_sources`, `search_knowledge`. Snippets bornés, pas de dump
document. En prod: Cloud Run IAM + bearer `MCP_READ_TOKEN`.

## GCP

Voir [docs/GCP.md](docs/GCP.md). Trois images non-root, endpoint régional
Vertex `{REGION}-aiplatform.googleapis.com:443`, trigger SA = `run.invoker`
uniquement.
