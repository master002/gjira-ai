package com.gjira.ingest.repository;

import com.gjira.ingest.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<TenantEntity, String> {

    default boolean existsById(UUID id) {
        return id != null && existsById(id.toString());
    }
}
