package com.gjira.ingest.staticingest;

import com.gjira.ingest.model.StandardInteractionFormat;
import com.gjira.ingest.naming.StaticFileNamingUtil;
import com.gjira.ingest.normalizer.UniversalNormalizer;
import com.gjira.ingest.publish.IngestPublisher;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Handles static document ingestion: text extraction, naming, delta check, publish.
 */
@Service
public class StaticIngestionService {

    private static final Logger log = LoggerFactory.getLogger(StaticIngestionService.class);
    private static final Tika TIKA = new Tika();

    private final IngestPublisher publisher;
    private final UniversalNormalizer normalizer;

    public StaticIngestionService(IngestPublisher publisher, UniversalNormalizer normalizer) {
        this.publisher = publisher;
        this.normalizer = normalizer;
    }

    public StaticIngestionController.UploadResponse ingestUpload(UUID tenantId, String sourceId, MultipartFile file) {
        String text;
        try {
            text = TIKA.parseToString(file.getInputStream());
        } catch (Exception e) {
            log.warn("Tika parse failed for file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Failed to extract text from file", e);
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("No extractable text in file");
        }

        String ext = extension(file.getOriginalFilename());
        String fileName = StaticFileNamingUtil.buildStaticFileName("ACM", LocalDate.now(), ext);

        var format = normalizer.normalizeUpload(tenantId, sourceId, text, Map.of(
                "original_filename", file.getOriginalFilename(),
                "file_path", fileName
        ));
        publisher.publish(format);
        return new StaticIngestionController.UploadResponse(sourceId, "ingested", fileName);
    }

    public StandardInteractionFormat ingestText(UUID tenantId, String sourceId, String text, Map<String, Object> metadata) {
        return normalizer.normalizeUpload(tenantId, sourceId, text, metadata);
    }

    private static String extension(String name) {
        if (name == null) return "txt";
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1) : "txt";
    }
}
