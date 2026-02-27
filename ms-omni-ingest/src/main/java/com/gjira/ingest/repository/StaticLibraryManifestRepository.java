package com.gjira.ingest.repository;

import com.gjira.ingest.entity.StaticLibraryManifestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface StaticLibraryManifestRepository extends JpaRepository<StaticLibraryManifestEntity, String> {

    Optional<StaticLibraryManifestEntity> findByTenantIdAndSourceTypeAndSourceId(
            String tenantId, String sourceType, String sourceId);

    @Modifying
    @Query("""
        UPDATE StaticLibraryManifestEntity m SET m.contentHash = :contentHash, m.filePath = :filePath, m.lastModified = :lastModified, m.ingestedAt = CURRENT_TIMESTAMP
        WHERE m.tenantId = :tenantId AND m.sourceType = :sourceType AND m.sourceId = :sourceId
        """)
    int updateExisting(@Param("tenantId") String tenantId, @Param("sourceType") String sourceType,
                       @Param("sourceId") String sourceId, @Param("contentHash") String contentHash,
                       @Param("filePath") String filePath, @Param("lastModified") Instant lastModified);

    @Transactional
    default void upsert(String tenantId, String sourceType, String sourceId, String contentHash, String filePath, Instant lastModified) {
        int updated = updateExisting(tenantId, sourceType, sourceId, contentHash, filePath, lastModified);
        if (updated == 0) {
            var e = new StaticLibraryManifestEntity();
            e.setTenantId(tenantId);
            e.setSourceType(sourceType);
            e.setSourceId(sourceId);
            e.setContentHash(contentHash);
            e.setFilePath(filePath);
            e.setLastModified(lastModified);
            save(e);
        }
    }
}
