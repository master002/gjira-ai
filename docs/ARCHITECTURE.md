# GJIRA.AI GLOBAL PROTOCOL — Technical Architecture

**Version:** 0.1.0  
**Design Principle:** Zero-Trust | Privacy-by-Design | Cost-Obsessed  
**Scale Target:** 10-Lakh (1M) users | Near-zero marginal cost

---

## PART 1: DUAL-STREAM INGESTION ARCHITECTURE

### Stream A: DYNAMIC CONTEXT (6-Month Rolling Window)

| Attribute | Specification |
|-----------|---------------|
| **Sources** | Slack, Teams, Zoom Chats, Meeting Transcripts |
| **Retention** | 6 months rolling (configurable per tenant) |
| **Primary Use** | Daily tasking, sprint queries, standup synthesis |
| **Output Naming** | `[YYYY-MM-DD]_[Company_Initials]_DYNAMIC.docx` |
| **Storage Tier** | Hot vector store (Qdrant/Milvus), compressed archive in R2 |
| **Cost Driver** | Vector index size, inference calls — **minimize both** |

**Ingestion Cadence:** Real-time webhooks (debounced 5min) + hourly batch reconciliation.

---

### Stream B: STATIC KNOWLEDGE (Sovereign Library)

| Attribute | Specification |
|-----------|---------------|
| **Sources** | Confluence API, SharePoint REST API, PDF/Word/Excel/Text uploads |
| **Retention** | Immutable until explicit refresh |
| **Primary Use** | Source-of-truth for policies, runbooks, technical docs |
| **Output Naming** | `[Latest_Date]_[Company_3_Letter_Initials]_STATIC.[ext]` |
| **Authority Level** | HIGH — never mixed with Dynamic in same retrieval path without explicit weighting |
| **Cost Driver** | Scrape frequency — **ingest ONCE per link; delta-only on change** |

**Isolation Rule:** Static and Dynamic vectors live in **separate collections** in the Shadow DB. Retrieval must specify stream type to avoid cross-contamination (hallucination mitigation).

---

## PART 2: HYBRID MICROSERVICES ARCHITECTURE

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           OMNI-INGESTION GATEWAY (MS-1)                          │
│  Webhooks: Slack | Teams | Zoom | Confluence | SharePoint → Standard Interaction │
└─────────────────────────────────────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         DUAL-CORE BRAIN (MS-2)                                   │
│  Edge LLM → Shadow DB (Dynamic_Vectors | Static_Library_Vectors) → R2 Archives   │
└─────────────────────────────────────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      GOVERNANCE & SKILL HUB (MS-3)                               │
│  Skill-Evolution | Cost-Decay Billing | Resolution Pre-drafts (Static + History) │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### MS-1: OMNI-INGESTION GATEWAY

- **Type:** Stateless, horizontally scalable
- **Runtime:** Spring Boot 3.x + Spring AI
- **Function:** Normalize all platform payloads into `StandardInteractionFormat` (JSON schema)
- **Webhook Endpoints:** `/webhook/slack`, `/webhook/teams`, `/webhook/zoom`, `/webhook/confluence`, `/webhook/sharepoint`
- **Output:** Publish to internal queue (Kafka/RabbitMQ) with `stream_type: DYNAMIC | STATIC`
- **Reject:** Any payload not mappable to schema within 3 retries — dead-letter to S3/R2 for manual review

### MS-2: DUAL-CORE BRAIN

- **Edge Path:** Browser-hosted LLM (WebLLM/Transformers.js) for low-latency inference on local context
- **Server Path:** Fallback to hosted LLM only when Edge unavailable or confidence < threshold
- **Vector Store:** Qdrant/Milvus with tenant-scoped collections; Dynamic vs Static **physically separated**
- **R2 Storage:** Zipped + AES-256 encrypted Brain Assets; zero egress cost
- **Oracle Logic:** RAG retrieval → Static for policy/runbook; Dynamic for recent discussions; fused response with source attribution

### MS-3: GOVERNANCE & SKILL HUB

- **Skill-Evolution:** Track per-user improvement (accuracy, resolution speed) via `User_Skill_Logs`
- **Cost-Decay Billing:** Usage-based; Edge inference = near-zero cost; server inference = billable
- **Resolution Pre-drafts:** Match recurring bug patterns against Static library + historical Dynamic fixes; propose drafts for Human-in-the-Loop approval

---

## PART 3: ADMIN GOVERNANCE & PRODUCT HEALTH

### Health Telemetry Dashboard

| Metric | Definition | Target |
|--------|------------|--------|
| **SLA Recovery Rate** | % of incidents resolved within SLA window | >95% |
| **Inference Efficiency** | Edge vs Server inference ratio | Edge >70% |
| **Knowledge Integrity** | Dynamic vs Static balance (ratio of retrievals) | Configurable per tenant |
| **Vector Freshness** | Age of oldest Dynamic vector per tenant | <6 months |

### Reliability Guardrail

- **Confidence Threshold:** 90% (configurable)
- **Action:** If AI confidence < 90% → **Human-in-the-Loop** mandatory
- **No autonomous:** Escalation, PII exposure, or policy-change without human approval

---

## Rejection Criteria

Any feature that:
1. Does not contribute to the "Local Brain" (knowledge accumulation or inference improvement)
2. Cannot be offloaded to the Edge
3. Increases marginal cost without proportional value
4. Violates privacy-by-design (e.g., raw PII in logs, cross-tenant data leak)

**→ REJECT**
