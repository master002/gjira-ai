package com.gjira.ingest.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "gjira.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path basePath;

    public LocalStorageService(@Value("${gjira.storage.base-path:./data/gjira}") String basePathStr) {
        this.basePath = Path.of(basePathStr);
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            log.warn("Could not create storage dir: {}", e.getMessage());
        }
    }

    @Override
    public void store(UUID tenantId, String filePath, byte[] content) {
        try {
            Path target = basePath.resolve(tenantId.toString()).resolve(filePath);
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new RuntimeException("Storage write failed: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] read(UUID tenantId, String filePath) throws IOException {
        Path target = basePath.resolve(tenantId.toString()).resolve(filePath);
        return Files.readAllBytes(target);
    }
}
