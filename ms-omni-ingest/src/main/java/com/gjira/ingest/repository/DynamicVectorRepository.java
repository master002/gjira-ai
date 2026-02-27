package com.gjira.ingest.repository;

import com.gjira.ingest.entity.DynamicVectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DynamicVectorRepository extends JpaRepository<DynamicVectorEntity, String> {
}
