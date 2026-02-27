package com.gjira.ingest.storage;

import java.io.IOException;
import java.util.UUID;

/**
 * Abstraction for static document storage. Local filesystem (dev) or R2/S3 (prod).
 */
public interface StorageService {

    void store(UUID tenantId, String filePath, byte[] content);

    byte[] read(UUID tenantId, String filePath) throws IOException;
}
