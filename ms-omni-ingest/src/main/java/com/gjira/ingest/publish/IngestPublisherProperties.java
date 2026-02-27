package com.gjira.ingest.publish;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gjira.ingest.publish")
public class IngestPublisherProperties {

    private boolean kafkaEnabled = false;

    public boolean isKafkaEnabled() {
        return kafkaEnabled;
    }

    public void setKafkaEnabled(boolean kafkaEnabled) {
        this.kafkaEnabled = kafkaEnabled;
    }
}
