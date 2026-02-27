package com.gjira.ingest.repository;

import com.gjira.ingest.entity.StaticLibraryVectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaticLibraryVectorRepository extends JpaRepository<StaticLibraryVectorEntity, String> {

    @Modifying
    @Query("DELETE FROM StaticLibraryVectorEntity v WHERE v.tenantId = :tenantId AND v.sourceType = :sourceType AND v.sourceId = :sourceId")
    void deleteByTenantIdAndSourceTypeAndSourceId(@Param("tenantId") String tenantId, @Param("sourceType") String sourceType, @Param("sourceId") String sourceId);
}
