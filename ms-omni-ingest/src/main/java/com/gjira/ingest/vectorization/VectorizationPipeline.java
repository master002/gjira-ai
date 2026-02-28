package com.gjira.ingest.vectorization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gjira.ingest.entity.DynamicVectorEntity;
import com.gjira.ingest.entity.StaticLibraryVectorEntity;
import com.gjira.ingest.model.StandardInteractionFormat;
import com.gjira.ingest.model.StandardInteractionFormat.StreamType;
import com.gjira.ingest.repository.DynamicVectorRepository;
import com.gjira.ingest.repository.StaticLibraryManifestRepository;
import com.gjira.ingest.repository.StaticLibraryVectorRepository;
import com.gjira.ingest.repository.TenantRepository;
import com.gjira.ingest.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Chunk → embed → persist to Shadow DB.
 * For dev: embedding is placeholder (zeros); production will use real embedding model.
 */
@Service
public class VectorizationPipeline {

    private static final Logger log = LoggerFactory.getLogger(VectorizationPipeline.class);

    private static final int CHUNK_SIZE = 1000;
    private static final int EMBEDDING_DIM = 1536;

    private final DynamicVectorRepository dynamicRepo;
    private final StaticLibraryVectorRepository staticRepo;
    private final StaticLibraryManifestRepository manifestRepo;
    private final TenantRepository tenantRepo;
    private final StorageService storageService;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public VectorizationPipeline(
            DynamicVectorRepository dynamicRepo,
            StaticLibraryVectorRepository staticRepo,
            StaticLibraryManifestRepository manifestRepo,
            TenantRepository tenantRepo,
            StorageService storageService,
            EmbeddingService embeddingService,
            ObjectMapper objectMapper
    ) {
        this.dynamicRepo = dynamicRepo;
        this.staticRepo = staticRepo;
        this.manifestRepo = manifestRepo;
        this.tenantRepo = tenantRepo;
        this.storageService = storageService;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(StandardInteractionFormat format) {
        if (!tenantRepo.existsById(format.tenantId())) {
            log.warn("Tenant {} not found, skipping", format.tenantId());
            return;
        }

        if (format.streamType() == StreamType.DYNAMIC) {
            persistDynamic(format);
        } else {
            persistStatic(format);
        }
    }

    private void persistDynamic(StandardInteractionFormat format) {
        String text = format.content().text();
        if (text == null || text.isBlank()) return;

        String metadataJson = serializeMetadata(format.content().metadata());

        var chunks = ChunkUtils.chunk(text, CHUNK_SIZE);
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            float[] embedding = embeddingService.embed(chunk);
            var entity = new DynamicVectorEntity(
                    null, format.tenantId().toString(), format.sourceType().name(), format.sourceId(),
                    format.contentHash(), chunk, embedding, Instant.now(), format.eventTs(), metadataJson
            );
            dynamicRepo.save(entity);
        }
        log.debug("Persisted {} dynamic chunks for tenant={} source={}", chunks.size(),
                format.tenantId(), format.sourceId());
    }

    private void persistStatic(StandardInteractionFormat format) {
        // 1. CHECK MANIFEST FOR DELTA (Cost-Obsessed Rule)
        var manifestOpt = manifestRepo.findByTenantIdAndSourceTypeAndSourceId(
                format.tenantId().toString(), format.sourceType().name(), format.sourceId());

        if (manifestOpt.isPresent() && manifestOpt.get().getContentHash().equals(format.contentHash())) {
            log.info("Cost-Decay Trigger: Skipping vectorization for unchanged content (tenant={}, source={})", 
                    format.tenantId(), format.sourceId());
            return;
        }

        String text = format.content().text();
        if (text == null || text.isBlank()) return;

        // 2. PURGE OLD VECTORS ONLY ON CHANGE
        staticRepo.deleteByTenantIdAndSourceTypeAndSourceId(
                format.tenantId().toString(), format.sourceType().name(), format.sourceId());

        // 3. ARCHIVE TO R2 (Zero-Egress Strategy)
        String filePath = format.sourceId() + "_STATIC.txt";
        storageService.store(format.tenantId(), filePath, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // 4. CHUNK AND EMBED
        var chunks = ChunkUtils.chunk(text, CHUNK_SIZE);
        String metadataJson = serializeMetadata(format.content().metadata());

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            float[] embedding = embeddingService.embed(chunk); // Costly operation saved by step 1
            var entity = new StaticLibraryVectorEntity(
                    null, format.tenantId().toString(), format.sourceType().name(), format.sourceId(),
                    i, format.contentHash(), chunk, embedding, filePath, Instant.now(), metadataJson
            );
            staticRepo.save(entity);
        }

        // 5. UPDATE MANIFEST
        manifestRepo.upsert(format.tenantId().toString(), format.sourceType().name(), format.sourceId(),
                format.contentHash(), filePath, format.eventTs());
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata, dropping it: {}", e.getMessage());
            return null;
        }
    }
}
