# 30-Day Build Plan

**Priority:** Universal Normalizer (Standard Interaction Format) + Static Document Scraper  
**Constraint:** Brutally technical; reject features that don't feed the Local Brain or Edge path.

---

## Phase 1: Foundation (Days 1–7)

### Week 1 Objectives

| Day | Deliverable | Output |
|-----|-------------|--------|
| 1–2 | Project scaffold | Spring Boot 3.2 + Java 21; Maven; `ms-omni-ingest` module |
| 2–3 | Standard Interaction Format | JSON schema + Java POJOs for normalized payload |
| 3–4 | Webhook stubs | `/webhook/slack`, `/webhook/teams`, `/webhook/zoom`, `/webhook/confluence`, `/webhook/sharepoint` — accept payload, validate, return 202 |
| 5–6 | Shadow DB bootstrap | Run schema; tenant seed; basic JPA repos |
| 7 | Universal Normalizer (core) | Map at least Slack + Confluence payloads → Standard Interaction Format |

### Standard Interaction Format (Schema)

```json
{
  "id": "uuid",
  "tenant_id": "uuid",
  "stream_type": "DYNAMIC | STATIC",
  "source_type": "slack | teams | zoom | confluence | sharepoint | upload",
  "source_id": "string",
  "event_ts": "ISO8601",
  "content": { "text": "string", "metadata": {} },
  "content_hash": "sha256"
}
```

---

## Phase 2: Static Document Scraper (Days 8–21)

### Week 2–3 Objectives

| Day | Deliverable | Output |
|-----|-------------|--------|
| 8–9 | Confluence adapter | Fetch page by ID; extract HTML body → text; hash |
| 10–11 | SharePoint adapter | Graph API client; fetch document content; Tika extraction |
| 12 | Direct upload | Multipart endpoint; Tika; save to temp; hash |
| 13 | Naming + manifest | `[YYYY-MM-DD]_[XXX]_STATIC.[ext]`; `static_library_manifest` upsert |
| 14 | Delta detection | Compare content_hash; skip if unchanged |
| 15–16 | R2 storage client | S3-compatible upload; encrypted archive pointer |
| 17–18 | Vectorization pipeline | Chunk → embed → insert into `static_library_vectors` |
| 19 | Integration test | End-to-end: Confluence URL → file in R2 → vector in DB |
| 20–21 | Observability | Metrics, DLQ, retry policy |

---

## Phase 3: Normalizer Completion + Handoff (Days 22–30)

### Week 4 Objectives

| Day | Deliverable | Output |
|-----|-------------|--------|
| 22–23 | Full Universal Normalizer | All 5 source types → Standard Interaction Format |
| 24 | Message queue | Publish normalized events to Kafka/RabbitMQ |
| 25–26 | Dynamic ingest pipeline | Consume from queue; chunk; embed; insert `dynamic_vectors` |
| 27 | Health telemetry stub | Basic metrics endpoint: ingest rate, queue depth |
| 28–29 | Documentation | API spec, runbook, env vars |
| 30 | Handoff | Demo: Confluence scrape + Slack webhook → vectors in Shadow DB |

---

## Out of Scope (30-Day)

- Edge inference (WebLLM)
- Governance & Skill Hub (MS-3)
- Human-in-the-Loop UI
- Global Brain export

---

## Success Criteria

1. **Universal Normalizer:** All 5 webhook types produce valid Standard Interaction Format.
2. **Static Scraper:** Confluence + SharePoint + Upload → correctly named files in R2 + vectors in `static_library_vectors`.
3. **Delta skip:** Re-scrape unchanged Confluence page → no duplicate write.
