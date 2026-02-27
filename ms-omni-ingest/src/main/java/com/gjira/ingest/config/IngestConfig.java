package com.gjira.ingest.config;

import com.gjira.ingest.publish.IngestPublisherProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IngestPublisherProperties.class)
public class IngestConfig {
}
