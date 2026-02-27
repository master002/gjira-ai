package com.gjira.ingest.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * Cloudflare R2 storage (S3-compatible). Zero egress cost.
 */
@Service
@ConditionalOnProperty(prefix = "gjira.storage", name = "type", havingValue = "r2")
public class R2StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(R2StorageService.class);

    private final S3Client s3Client;
    private final String bucket;

    public R2StorageService(S3Client s3Client,
                            @Value("${gjira.storage.r2.bucket:gjira-static}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void store(UUID tenantId, String filePath, byte[] content) {
        String key = tenantId + "/" + filePath;
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).build(),
                RequestBody.fromBytes(content)
        );
    }

    @Override
    public byte[] read(UUID tenantId, String filePath) throws IOException {
        String key = tenantId + "/" + filePath;
        try {
            return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())
                    .readAllBytes();
        } catch (Exception e) {
            throw new IOException("R2 read failed: " + e.getMessage(), e);
        }
    }
}
