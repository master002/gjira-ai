# GJIRA.AI

Zero-Trust, Omni-channel Project Oracle — dismantling manual Jira overhead through a Self-Sovereign "Local Brain" at 10-Lakh scale with near-zero marginal cost.

## Architecture

- **Dual-Stream Ingestion:** Dynamic (Slack/Teams/Zoom) + Static (Confluence/SharePoint)
- **Hybrid Microservices:** Omni-Ingestion Gateway | Dual-Core Brain | Governance & Skill Hub
- **Storage:** Shadow DB (PostgreSQL + pgvector) + Cloudflare R2 (encrypted archives)

## Docs

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Global Protocol — Parts 1–3 |
| [STATIC_LIBRARY_INGESTOR.md](docs/STATIC_LIBRARY_INGESTOR.md) | Java service spec for Confluence/SharePoint |
| [SHADOW_DB_SCHEMA.sql](docs/SHADOW_DB_SCHEMA.sql) | Multi-tenant schema |
| [LOCAL_TO_GLOBAL_STRATEGY.md](docs/LOCAL_TO_GLOBAL_STRATEGY.md) | Anonymized Global Brain strategy |
| [30_DAY_BUILD_PLAN.md](docs/30_DAY_BUILD_PLAN.md) | Build plan — Universal Normalizer + Static Scraper |
