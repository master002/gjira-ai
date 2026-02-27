# Static Library Ingestor — Java Service Specification

**Component:** MS-1 Extension / Dedicated Worker  
**Runtime:** Java 21 + Spring Boot 3.2+  
**Cost Target:** Ingest ONCE per link; delta-only on content hash change

---

## 1. Service Responsibility

Scrape Confluence and SharePoint sources, normalize content to a standard format, and persist with the correct naming convention:

```
[YYYY-MM-DD]_[XXX]_STATIC.[ext]
```

Where `XXX` = 3-letter company/tenant initials (e.g., `ACME`).

---

## 2. Source Adapters

### 2.1 Confluence Adapter

| Aspect | Specification |
|--------|---------------|
| **API** | Confluence Server/REST API v2 or Confluence Cloud REST API |
| **Auth** | Personal Access Token (PAT) or OAuth 2.0 |
| **Scope** | Space-scoped or page-ID list; exclude attachments by default (opt-in) |
| **Rate Limit** | Max 10 req/sec per tenant; exponential backoff on 429 |

**Confluence Content Extraction:**
- HTML body → strip markup, extract text
- Storage format: `.docx` (via Apache POI) or `.txt` for minimal cost
- Metadata: `page_id`, `space_key`, `last_modified`, `title`

**Endpoint Pattern:**
```
GET /wiki/rest/api/content/{id}?expand=body.storage,version
GET /wiki/rest/api/content?spaceKey={key}&limit=50
```

### 2.2 SharePoint Adapter

| Aspect | Specification |
|--------|---------------|
| **API** | Microsoft Graph API (preferred) or SharePoint REST API |
| **Auth** | Azure AD app registration; client credentials or delegated |
| **Scope** | Site/Drive/List; document libraries only |
| **Rate Limit** | Throttle to 2 req/sec per tenant; respect Retry-After |

**SharePoint Content Extraction:**
- `.docx`, `.xlsx`, `.pdf` → Apache Tika for text extraction
- `.txt` → direct read
- Metadata: `item_id`, `drive_id`, `last_modified`, `name`

**Graph API Pattern:**
```
GET /sites/{site-id}/drive/items/{item-id}/content
GET /sites/{site-id}/drive/root/children
```

### 2.3 Direct Upload (PDF, Word, Excel, Text)

- Accept multipart upload to `/ingest/static/upload`
- Validate file type and size (<10MB per file)
- Extract text via Tika; persist as `.docx` or `.txt`

---

## 3. Naming Convention Implementation

```java
public String buildStaticFileName(String tenantInitials, LocalDate latestDate, String extension) {
    tenantInitials = (tenantInitials == null ? "XXX" : tenantInitials).toUpperCase();
    if (tenantInitials.length() < 3) {
        tenantInitials = (tenantInitials + "XXX").substring(0, 3);
    } else if (tenantInitials.length() > 3) {
        tenantInitials = tenantInitials.substring(0, 3);
    }
    return String.format("%s_%s_STATIC.%s",
        latestDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
        tenantInitials,
        extension);
}
```

**Examples:**
- `2025-02-27_ACM_STATIC.docx`
- `2025-02-27_XYZ_STATIC.txt`

---

## 4. Idempotency & Delta Detection

| Rule | Implementation |
|------|----------------|
| **Content Hash** | SHA-256 of normalized text before storage |
| **Skip if Unchanged** | Compare hash with last stored; skip write + vector update |
| **Change Detection** | Confluence: `version.number`; SharePoint: `lastModifiedDateTime` |
| **Deduplication** | `(tenant_id, source_type, source_id)` unique key |

**Flow:**
1. Fetch content from source
2. Compute `content_hash`
3. Query `Static_Library_Manifest` for existing `(tenant_id, source_type, source_id)`
4. If `existing_hash == content_hash` → skip
5. Else → persist file, update manifest, enqueue for vectorization

---

## 5. Java Service Structure (Spring Boot)

```
com.gjira.ingest.static
├── StaticIngestionService.java      # Orchestrator
├── adapters/
│   ├── ConfluenceAdapter.java       # Confluence REST client
│   ├── SharePointAdapter.java       # Graph/SharePoint client
│   └── DirectUploadAdapter.java     # Multipart handler
├── model/
│   ├── StaticDocument.java          # Internal representation
│   └── StaticManifestEntry.java     # Hash + metadata
├── storage/
│   ├── R2StorageClient.java         # Cloudflare R2 upload
│   └── ManifestRepository.java      # DB access
└── config/
    ├── ConfluenceProperties.java
    └── SharePointProperties.java
```

### Dependencies (Maven)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <!-- R2 is S3-compatible -->
    </dependency>
</dependencies>
```

---

## 6. Error Handling & Observability

- **Retry:** 3 attempts with exponential backoff for transient errors
- **Dead Letter:** Failed documents → `static_ingest_dlq` topic; alert on DLQ depth
- **Metrics:** `static_ingest_total`, `static_ingest_skipped_delta`, `static_ingest_failed`
- **Logging:** No PII; log `tenant_id`, `source_id`, `content_hash`, status only
