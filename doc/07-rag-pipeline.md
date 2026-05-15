# RAG Pipeline

How documents flow from Confluence into Qdrant, and how queries are answered.

## Indexing pipeline

```mermaid
flowchart LR
    A[Confluence page<br/>HTML body] --> B[SopUtil.chunkHtml]
    B --> C{Chunks<br/>Map heading→text}
    C --> D[Build Spring AI Document]
    D --> D1[id = UUID nameUUIDFromBytes title heading]
    D --> D2[metadata: source, spaceKey, title, heading]
    D --> E[Ollama nomic-embed-text]
    E --> F[768-dim vector]
    F --> G[(Qdrant upsert<br/>collection sop-v01)]
```

## Retrieval pipeline

```mermaid
flowchart LR
    Q[User query] --> EM[Ollama embed]
    EM --> V[768-dim query vector]
    F[Filters: title, heading] --> S
    V --> S[Qdrant similarity search<br/>topK = 5]
    S --> D[Top-K Documents]
    D --> P[Build prompt context]
    P --> L[OpenAI gpt-5.4 ChatClient]
    L --> R[Generated response<br/>+ optional MCP tool calls]
```

## Document schema in Qdrant

| Field | Description |
|---|---|
| `id` | Deterministic UUID from `title|heading` (idempotent upserts) |
| `vector` | 768-dim embedding from `nomic-embed-text` |
| `payload.text` | `Heading: <h>\n<chunk content>` |
| `payload.metadata.source` | `confluence` |
| `payload.metadata.spaceKey` | Confluence space key |
| `payload.metadata.title` | Confluence page title |
| `payload.metadata.heading` | Section heading within the page |
