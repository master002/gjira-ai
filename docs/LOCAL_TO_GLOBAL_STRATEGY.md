# Local-to-Global Transition Strategy

**Objective:** Produce anonymized, aggregated "Global Brain" summaries annually without cross-tenant data leakage.

---

## 1. Principles

| Principle | Implementation |
|-----------|----------------|
| **Privacy-First** | No raw content leaves tenant boundary; only aggregates + patterns |
| **Consent** | Opt-in per tenant for Global Brain contribution |
| **Anonymization** | Tenant ID → hashed cohort; no org names or PII |
| **Utility** | Patterns useful for product improvement, benchmark baselines, cross-industry insights |

---

## 2. Extractable Aggregates (Local → Export)

Each tenant's "Local Brain" exports **only**:

| Data Type | Description | Anonymization |
|-----------|-------------|---------------|
| **Resolution patterns** | Bug type → resolution action mapping (structure only) | Strip all identifiers; generalize labels |
| **Inference efficiency** | Edge vs Server ratio, avg latency | Per-tenant aggregate |
| **Knowledge balance** | Static vs Dynamic retrieval ratio | Per-tenant aggregate |
| **Skill evolution curve** | Aggregate accuracy/confidence trend | Per-tenant; no user IDs |
| **Common query taxonomies** | Top-level intent categories (e.g., "deployment", "incident") | Generic labels only |

**Excluded:**
- Raw text, embeddings, document content
- User IDs, org names, source IDs
- Any payload that could re-identify a tenant

---

## 3. Global Brain Aggregation Pipeline

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Tenant A     │     │ Tenant B     │     │ Tenant N     │
│ Local Brain  │     │ Local Brain  │     │ Local Brain  │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       ▼                    ▼                    ▼
┌─────────────────────────────────────────────────────────┐
│              ANONYMIZED EXPORT (Annual Job)              │
│  - Cohorts by industry/size (tenant opts into cohort)   │
│  - No reversible identifiers                            │
└─────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────┐
│              GLOBAL BRAIN AGGREGATOR                     │
│  - Merge patterns into global model                     │
│  - Publish benchmarks, best-practice summaries          │
│  - Feedback to improve default prompts / taxonomies     │
└─────────────────────────────────────────────────────────┘
```

---

## 4. Annual Cycle

| Phase | Timing | Action |
|-------|--------|--------|
| **Opt-in** | Q4 | Tenants consent to contribute anonymized aggregates |
| **Extract** | Jan 1 | Batch job per tenant; outputs to secure staging |
| **Transform** | Jan 2–7 | Anonymize, hash, aggregate by cohort |
| **Load** | Jan 8+ | Merge into Global Brain; purge staging |
| **Publish** | Feb | Release anonymized benchmarks, insights |

---

## 5. Output Artifacts

- **Global Resolution Playbook:** Merged, anonymized bug→fix patterns
- **Industry Benchmarks:** Inference efficiency, knowledge balance by cohort
- **Default Taxonomy Updates:** Refined intent labels for new tenants

---

## 6. Governance

- Data Processing Agreement (DPA) must cover Global Brain contribution
- Export format: signed, encrypted; decryption only in isolated aggregation env
- Audit log: all exports, aggregations, purges
