# ms-omni-ingest

Omni-Ingestion Gateway for GJIRA.AI — webhook receiver, Universal Normalizer, static ingestion, and vectorization pipeline.

## Requirements

- **JDK 21** (not JRE)
- Maven 3.8+

## Run

```bash
mvn -pl ms-omni-ingest spring-boot:run
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/webhook/slack` | Slack events → Dynamic stream |
| POST | `/webhook/teams` | Microsoft Teams events → Dynamic stream |
| POST | `/webhook/zoom` | Zoom events → Dynamic stream |
| POST | `/webhook/confluence` | Confluence page payloads → Static stream |
| POST | `/webhook/sharepoint` | SharePoint payloads → Static stream |
| POST | `/ingest/static/upload` | Multipart file upload → Static stream |
| POST | `/ingest/static/text` | JSON `{ "text": "...", "sourceId": "..." }` → Static stream |
| GET | `/actuator/health` | Health + vector counts |

## Headers

- `X-Tenant-Id`: UUID (default: `00000000-0000-0000-0000-000000000001`)

## Example

```bash
# Ingest text
curl -X POST http://localhost:8080/ingest/static/text \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 00000000-0000-0000-0000-000000000001" \
  -d '{"text":"Runbook: restart service X","sourceId":"runbook-001"}'

# Slack webhook
curl -X POST http://localhost:8080/webhook/slack \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 00000000-0000-0000-0000-000000000001" \
  -d '{"event":{"text":"Deploy done","channel_id":"C1","ts":"1234567890.123456"}}'
```

## Tests

```bash
mvn -pl ms-omni-ingest test
```

## Docker

```bash
# Build JAR
mvn -pl ms-omni-ingest package -DskipTests

# Build and run
docker compose up --build
```

## Confluence adapter (optional)

Set `gjira.confluence.enabled=true` and configure:

```yaml
gjira:
  confluence:
    enabled: true
    base-url: https://your-domain.atlassian.net
    token: <PAT>
```

Then: `POST /ingest/static/confluence` with `{ "pageId": "12345" }`

## R2 storage (optional)

Set `gjira.storage.type=r2` and configure endpoint, bucket, credentials. See `application-r2.yml`.
