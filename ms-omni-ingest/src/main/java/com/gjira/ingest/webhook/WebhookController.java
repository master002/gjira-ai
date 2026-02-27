package com.gjira.ingest.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.gjira.ingest.model.StandardInteractionFormat.SourceType;
import com.gjira.ingest.normalizer.UniversalNormalizer;
import com.gjira.ingest.publish.IngestPublisher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Stateless webhook receiver. Accepts platform payloads, normalizes, publishes.
 * Returns 202 immediately; processing is async.
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final UniversalNormalizer normalizer;
    private final IngestPublisher publisher;

    public WebhookController(UniversalNormalizer normalizer, IngestPublisher publisher) {
        this.normalizer = normalizer;
        this.publisher = publisher;
    }

    @PostMapping("/slack")
    public ResponseEntity<Void> slack(
            @RequestBody JsonNode payload,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader,
            HttpServletRequest req
    ) {
        return accept(SourceType.slack, payload, tenantIdHeader, req);
    }

    @PostMapping("/teams")
    public ResponseEntity<Void> teams(
            @RequestBody JsonNode payload,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader,
            HttpServletRequest req
    ) {
        return accept(SourceType.teams, payload, tenantIdHeader, req);
    }

    @PostMapping("/zoom")
    public ResponseEntity<Void> zoom(
            @RequestBody JsonNode payload,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader,
            HttpServletRequest req
    ) {
        return accept(SourceType.zoom, payload, tenantIdHeader, req);
    }

    @PostMapping("/confluence")
    public ResponseEntity<Void> confluence(
            @RequestBody JsonNode payload,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader,
            HttpServletRequest req
    ) {
        return accept(SourceType.confluence, payload, tenantIdHeader, req);
    }

    @PostMapping("/sharepoint")
    public ResponseEntity<Void> sharepoint(
            @RequestBody JsonNode payload,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader,
            HttpServletRequest req
    ) {
        return accept(SourceType.sharepoint, payload, tenantIdHeader, req);
    }

    private ResponseEntity<Void> accept(
            SourceType sourceType,
            JsonNode payload,
            String tenantIdHeader,
            HttpServletRequest req
    ) {
        UUID tenantId = resolveTenantId(tenantIdHeader);
        normalizer.normalize(sourceType, tenantId, payload)
                .ifPresentOrElse(
                        format -> publisher.publish(format),
                        () -> log.debug("Normalizer returned empty for source={} tenant={}", sourceType, tenantId)
                );
        return ResponseEntity.accepted().build();
    }

    private UUID resolveTenantId(String header) {
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
