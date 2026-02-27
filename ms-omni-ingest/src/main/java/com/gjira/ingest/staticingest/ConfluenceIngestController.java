package com.gjira.ingest.staticingest;

import com.gjira.ingest.model.StandardInteractionFormat.SourceType;
import com.gjira.ingest.normalizer.UniversalNormalizer;
import com.gjira.ingest.publish.IngestPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Trigger Confluence page ingest by page ID. Requires Confluence adapter enabled.
 */
@RestController
@RequestMapping("/ingest/static")
@ConditionalOnBean(ConfluenceAdapter.class)
public class ConfluenceIngestController {

    private final ConfluenceAdapter confluenceAdapter;
    private final UniversalNormalizer normalizer;
    private final IngestPublisher publisher;

    public ConfluenceIngestController(ConfluenceAdapter confluenceAdapter,
                                      UniversalNormalizer normalizer,
                                      IngestPublisher publisher) {
        this.confluenceAdapter = confluenceAdapter;
        this.normalizer = normalizer;
        this.publisher = publisher;
    }

    @PostMapping("/confluence")
    public ResponseEntity<ConfluenceIngestResponse> ingest(
            @RequestBody ConfluenceIngestRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader
    ) {
        if (request == null || request.pageId() == null || request.pageId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        UUID tenantId = resolveTenantId(tenantIdHeader);
        var page = confluenceAdapter.fetchPage(request.pageId().trim());

        if (page.plainText().isBlank()) {
            return ResponseEntity.accepted()
                    .body(new ConfluenceIngestResponse(request.pageId(), "skipped", "No content"));
        }

        var format = normalizer.normalizeStatic(tenantId, SourceType.confluence, request.pageId(), page.plainText(),
                Map.of("title", page.title()));
        publisher.publish(format);

        return ResponseEntity.accepted()
                .body(new ConfluenceIngestResponse(request.pageId(), "ingested", page.title()));
    }

    private UUID resolveTenantId(String header) {
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    public record ConfluenceIngestRequest(String pageId) {}
    public record ConfluenceIngestResponse(String pageId, String status, String title) {}
}
