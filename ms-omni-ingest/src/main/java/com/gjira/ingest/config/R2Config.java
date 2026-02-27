package com.gjira.ingest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

/**
 * S3 client for Cloudflare R2. R2 is S3-compatible; set endpoint to R2 URL.
 */
@Configuration
@ConditionalOnProperty(prefix = "gjira.storage", name = "type", havingValue = "r2")
public class R2Config {

    @Bean
    public S3Client s3Client(
            @Value("${gjira.storage.r2.endpoint}") String endpoint,
            @Value("${gjira.storage.r2.access-key-id}") String accessKeyId,
            @Value("${gjira.storage.r2.secret-access-key}") String secretAccessKey
    ) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .endpointOverride(URI.create(endpoint));
        return builder.build();
    }
}
