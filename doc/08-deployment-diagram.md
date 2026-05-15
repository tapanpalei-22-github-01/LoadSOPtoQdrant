# Deployment Diagram

Runtime topology of all moving pieces.

```mermaid
flowchart TB
    subgraph Dev["Developer workstation"]
        subgraph JVM["JVM (Java 21)"]
            APP[i-support-alerts<br/>Spring Boot :10001]
        end
        OLLAMA[Ollama daemon<br/>:11434<br/>nomic-embed-text]
        QDRANT[(Qdrant<br/>:6334<br/>collection sop-v01)]
        MCPSRV[MCP Server<br/>:9090]
        FE[Frontend<br/>:3000]
    end

    subgraph Corp["Corporate network"]
        CONF[Confluence<br/>confluence.dev.e2open.com]
    end

    subgraph Cloud["Internet / Gateway"]
        GW[LLM Gateway<br/>llm-gateway.wtg.zone<br/>gpt-5.4]
    end

    subgraph Remote["Remote server"]
        SSHD[lsp-dev02-fr4.blujay.global<br/>SSH :22]
    end

    FE -->|HTTP/SSE| APP
    APP -->|HTTP| OLLAMA
    APP -->|gRPC| QDRANT
    APP -->|SSE| MCPSRV
    APP -->|HTTPS| CONF
    APP -->|HTTPS| GW
    MCPSRV -->|SSH| SSHD
```

## Port summary

| Service | Port | Protocol |
|---|---|---|
| i-support-alerts | 10001 | HTTP |
| Frontend | 3000 | HTTP |
| Ollama | 11434 | HTTP |
| Qdrant | 6334 | gRPC |
| MCP server | 9090 | SSE/HTTP |
| Remote SSH | 22 | SSH |
