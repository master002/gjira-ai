package com.gjira.ingest.vectorization;

import com.gjira.ingest.entity.DynamicVectorEntity;
import com.gjira.ingest.entity.StaticLibraryVectorEntity;
import com.gjira.ingest.model.StandardInteractionFormat;
import com.gjira.ingest.model.StandardInteractionFormat.StreamType;
import com.gjira.ingest.repository.DynamicVectorRepository;
import com.gjira.ingest.repository.StaticLibraryVectorRepository;
import com.gjira.ingest.repository.StaticLibraryManifestRepository;
import com.gjira.ingest.repository.TenantRepository;
import com.gjira.ingest.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

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

    public VectorizationPipeline(
            DynamicVectorRepository dynamicRepo,
            StaticLibraryVectorRepository staticRepo,
            StaticLibraryManifestRepository manifestRepo,
            TenantRepository tenantRepo,
            StorageService storageService,
            EmbeddingService embeddingService
    ) {
        this.dynamicRepo = dynamicRepo;
        this.staticRepo = staticRepo;
        this.manifestRepo = manifestRepo;
        this.tenantRepo = tenantRepo;
        this.storageService = storageService;
        this.embeddingService = embeddingService;
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

        var chunks = ChunkUtils.chunk(text, CHUNK_SIZE);
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            float[] embedding = embeddingService.embed(chunk);
            var entity = new DynamicVectorEntity(
                    null, format.tenantId().toString(), format.sourceType().name(), format.sourceId(),
                    format.contentHash(), chunk, embedding, Instant.now(), format.eventTs(), null
            );
            dynamicRepo.save(entity);
        }
        log.debug("Persisted {} dynamic chunks for tenant={} source={}", chunks.size(),
                format.tenantId(), format.sourceId());
    }

    private void persistStatic(StandardInteractionFormat format) {
        var manifestOpt = manifestRepo.findByTenantIdAndSourceTypeAndSourceId(
                format.tenantId().toString(), format.sourceType().name(), format.sourceId());
        if (manifestOpt.isPresent() && manifestOpt.get().getContentHash().equals(format.contentHash())) {
            log.debug("Static skip (unchanged): tenant={} source_type={} source_id={}",
                    format.tenantId(), format.sourceType(), format.sourceId());
            return;
        }

        String text = format.content().text();
        if (text == null || text.isBlank()) return;

        staticRepo.deleteByTenantIdAndSourceTypeAndSourceId(format.tenantId().toString(), format.sourceType().name(), format.sourceId());

        String filePath = format.sourceId() + "_STATIC.txt";
        storageService.store(format.tenantId(), filePath, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        var chunks = ChunkUtils.chunk(text, CHUNK_SIZE);
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            float[] embedding = embeddingService.embed(chunk);
            var entity = new StaticLibraryVectorEntity(
                    null, format.tenantId().toString(), format.sourceType().name(), format.sourceId(),
                    i, format.contentHash(), chunk, embedding, filePath, Instant.now(), null
            );
            staticRepo.save(entity);
        }

        manifestRepo.upsert(format.tenantId().toString(), format.sourceType().name(), format.sourceId(),
                format.contentHash(), filePath, format.eventTs());
        log.debug("Persisted {} static chunks for tenant={} source={}", chunks.size(),
                format.tenantId(), format.sourceId());
    }
}
