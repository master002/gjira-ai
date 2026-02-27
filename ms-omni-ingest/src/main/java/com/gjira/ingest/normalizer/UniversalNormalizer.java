package com.gjira.ingest.normalizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.gjira.ingest.model.StandardInteractionFormat;
import com.gjira.ingest.model.StandardInteractionFormat.Content;
import com.gjira.ingest.model.StandardInteractionFormat.SourceType;
import com.gjira.ingest.model.StandardInteractionFormat.StreamType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Maps platform webhook payloads into Standard Interaction Format.
 * Supports Slack, Teams, Zoom, Confluence, SharePoint.
 */
@Component
public class UniversalNormalizer {

    private static final Logger log = LoggerFactory.getLogger(UniversalNormalizer.class);

    public Optional<StandardInteractionFormat> normalize(
            SourceType sourceType,
            UUID tenantId,
            JsonNode payload
    ) {
        try {
            return switch (sourceType) {
                case slack -> normalizeSlack(tenantId, payload);
                case teams -> normalizeTeams(tenantId, payload);
                case zoom -> normalizeZoom(tenantId, payload);
                case confluence -> normalizeConfluence(tenantId, payload);
                case sharepoint -> normalizeSharePoint(tenantId, payload);
                case upload -> Optional.empty(); // upload uses direct flow, not webhook
            };
        } catch (Exception e) {
            log.warn("Normalization failed for source={} tenant={}: {}", sourceType, tenantId, e.getMessage());
            return Optional.empty();
        }
    }

    public StandardInteractionFormat normalizeUpload(UUID tenantId, String sourceId, String text, Map<String, Object> metadata) {
        return normalizeStatic(tenantId, SourceType.upload, sourceId, text, metadata);
    }

    public StandardInteractionFormat normalizeStatic(UUID tenantId, SourceType sourceType, String sourceId, String text, Map<String, Object> metadata) {
        String contentHash = sha256(text);
        Instant now = Instant.now();
        return new StandardInteractionFormat(
                UUID.randomUUID(),
                tenantId,
                StreamType.STATIC,
                sourceType,
                sourceId,
                now,
                new Content(text, metadata),
                contentHash
        );
    }

    private Optional<StandardInteractionFormat> normalizeSlack(UUID tenantId, JsonNode p) {
        JsonNode event = p.has("event") ? p.get("event") : p;
        if (event == null) return Optional.empty();

        String text = textOf(event, "text");
        if (text == null || text.isBlank()) text = textOf(event, "message", "text");
        if (text == null || text.isBlank()) return Optional.empty();

        String ts = textOf(event, "ts");
        String sourceId = textOf(event, "channel_id") + ":" + (ts != null ? ts : UUID.randomUUID());

        return build(tenantId, SourceType.slack, StreamType.DYNAMIC, sourceId, text, eventTs(event));
    }

    private Optional<StandardInteractionFormat> normalizeTeams(UUID tenantId, JsonNode p) {
        String text = textOf(p, "body", "content") != null ? textOf(p, "body", "content")
                : textOf(p, "text") != null ? textOf(p, "text") : textOf(p, "content");
        if (text == null || text.isBlank()) return Optional.empty();

        String sourceId = textOf(p, "id") != null ? textOf(p, "id") : UUID.randomUUID().toString();
        return build(tenantId, SourceType.teams, StreamType.DYNAMIC, sourceId, text, eventTs(p));
    }

    private Optional<StandardInteractionFormat> normalizeZoom(UUID tenantId, JsonNode p) {
        String text = textOf(p, "payload", "object", "transcript") != null
                ? textOf(p, "payload", "object", "transcript")
                : textOf(p, "transcript") != null ? textOf(p, "transcript") : textOf(p, "content");
        if (text == null || text.isBlank()) return Optional.empty();

        String sourceId = textOf(p, "payload", "object", "id") != null
                ? textOf(p, "payload", "object", "id")
                : textOf(p, "event_id") != null ? textOf(p, "event_id") : UUID.randomUUID().toString();
        return build(tenantId, SourceType.zoom, StreamType.DYNAMIC, sourceId, text, eventTs(p));
    }

    private Optional<StandardInteractionFormat> normalizeConfluence(UUID tenantId, JsonNode p) {
        String text = textOf(p, "body", "storage", "value") != null
                ? textOf(p, "body", "storage", "value")
                : textOf(p, "body", "view", "value") != null ? textOf(p, "body", "view", "value")
                : textOf(p, "content");
        if (text == null || text.isBlank()) return Optional.empty();

        String sourceId = textOf(p, "id") != null ? textOf(p, "id") : textOf(p, "page_id");
        if (sourceId == null) sourceId = UUID.randomUUID().toString();
        return build(tenantId, SourceType.confluence, StreamType.STATIC, sourceId, text, eventTs(p));
    }

    private Optional<StandardInteractionFormat> normalizeSharePoint(UUID tenantId, JsonNode p) {
        String text = textOf(p, "content") != null ? textOf(p, "content")
                : textOf(p, "value", "content") != null ? textOf(p, "value", "content")
                : textOf(p, "body");
        if (text == null || text.isBlank()) return Optional.empty();

        String sourceId = textOf(p, "id") != null ? textOf(p, "id")
                : textOf(p, "driveItem", "id") != null ? textOf(p, "driveItem", "id")
                : UUID.randomUUID().toString();
        return build(tenantId, SourceType.sharepoint, StreamType.STATIC, sourceId, text, eventTs(p));
    }

    private Optional<StandardInteractionFormat> build(
            UUID tenantId, SourceType sourceType, StreamType streamType,
            String sourceId, String text, Instant eventTs
    ) {
        String contentHash = sha256(text);
        return Optional.of(new StandardInteractionFormat(
                UUID.randomUUID(),
                tenantId,
                streamType,
                sourceType,
                sourceId,
                eventTs,
                new Content(text, null),
                contentHash
        ));
    }

    private String textOf(JsonNode n, String... keys) {
        JsonNode cur = n;
        for (String k : keys) {
            if (cur == null) return null;
            cur = cur.get(k);
        }
        return cur != null ? cur.asText() : null;
    }

    private Instant eventTs(JsonNode p) {
        JsonNode ts = p.path("event_ts").isMissingNode() ? p.path("ts") : p.path("event_ts");
        if (!ts.isMissingNode()) {
            try {
                double d = ts.asDouble();
                return Instant.ofEpochMilli((long) (d * 1000));
            } catch (Exception ignored) {}
        }
        JsonNode dt = p.path("lastModifiedDateTime").isMissingNode() ? p.path("version", "when")
                : p.path("lastModifiedDateTime");
        if (!dt.isMissingNode()) {
            try {
                return Instant.parse(dt.asText());
            } catch (Exception ignored) {}
        }
        return Instant.now();
    }

    public static String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((input != null ? input : "").getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
