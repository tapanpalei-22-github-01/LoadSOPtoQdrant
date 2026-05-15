# i-support-alerts — Architecture Overview

## What it does

`i-support-alerts` is an AI-powered alert-support assistant. It ingests Standard Operating Procedure (SOP) pages from Confluence, indexes them in a Qdrant vector store, and uses an LLM (OpenAI `gpt-5.4` via a custom gateway) plus an MCP tool server to walk a support engineer through analyzing and fixing alerts referenced by codes such as `EDG_002`, `ITS_001`, `MTP_003`.

## Tech stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 4.0.6 |
| AI framework | Spring AI 2.0.0-M4 |
| Chat model | OpenAI `gpt-5.4` (custom gateway) |
| Embedding model | Ollama `nomic-embed-text` (local) |
| Vector store | Qdrant (`localhost:6334`, collection `sop-v01`) |
| Tooling | MCP client → MCP server (`localhost:9090`) |
| Document source | Confluence REST API |
| Frontend | `http://localhost:3000` (CORS-allowed) |
| Server port | `10001` |

## High-level modules

```
controller/       REST entry points (Auto, Sop, Task)
service/          SopService — Confluence + chunking + Qdrant upsert
config/           ChatConfig (models), CorsConfig (CORS)
utils/            SopUtil (HTML chunking), LoggingInterceptor
```

See:
- [02-component-diagram.md](02-component-diagram.md)
- [03-class-diagram.md](03-class-diagram.md)
- [04-sop-load-flow.md](04-sop-load-flow.md)
- [05-auto-chat-flow.md](05-auto-chat-flow.md)
- [06-task-chat-flow.md](06-task-chat-flow.md)
- [07-rag-pipeline.md](07-rag-pipeline.md)
- [08-deployment-diagram.md](08-deployment-diagram.md)
- [09-state-diagram.md](09-state-diagram.md)
