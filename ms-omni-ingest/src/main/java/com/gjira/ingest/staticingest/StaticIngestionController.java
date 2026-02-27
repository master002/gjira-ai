package com.gjira.ingest.staticingest;

import com.gjira.ingest.publish.IngestPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * Direct upload for static documents. Accepts text or file upload.
 */
@RestController
@RequestMapping("/ingest/static")
public class StaticIngestionController {

    private static final Logger log = LoggerFactory.getLogger(StaticIngestionController.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final StaticIngestionService staticIngestionService;
    private final IngestPublisher publisher;

    public StaticIngestionController(StaticIngestionService staticIngestionService, IngestPublisher publisher) {
        this.staticIngestionService = staticIngestionService;
        this.publisher = publisher;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader,
            @RequestParam(value = "sourceId", required = false) String sourceId
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.status(413).build();
        }

        UUID tenantId = resolveTenantId(tenantIdHeader);
        String effectiveSourceId = sourceId != null && !sourceId.isBlank()
                ? sourceId : "upload_" + UUID.randomUUID();

        var result = staticIngestionService.ingestUpload(tenantId, effectiveSourceId, file);
        return ResponseEntity.accepted().body(result);
    }

    @PostMapping("/text")
    public ResponseEntity<UploadResponse> ingestText(
            @RequestBody TextIngestRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader
    ) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        UUID tenantId = resolveTenantId(tenantIdHeader);
        String sourceId = request.sourceId() != null && !request.sourceId().isBlank()
                ? request.sourceId() : "text_" + UUID.randomUUID();

        var format = staticIngestionService.ingestText(tenantId, sourceId, request.text(), request.metadata());
        publisher.publish(format);
        return ResponseEntity.accepted().body(new UploadResponse(sourceId, "ingested", null));
    }

    private UUID resolveTenantId(String header) {
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    public record TextIngestRequest(String text, String sourceId, Map<String, Object> metadata) {}
    public record UploadResponse(String sourceId, String status, String filePath) {}
}
