package com.gjira.ingest.publish;

import com.gjira.ingest.model.StandardInteractionFormat;
import com.gjira.ingest.vectorization.VectorizationPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes normalized interactions to Kafka and triggers immediate vectorization.
 * In dev (no Kafka), falls back to direct vectorization.
 */
@Service
public class IngestPublisher {

    private static final Logger log = LoggerFactory.getLogger(IngestPublisher.class);

    public static final String TOPIC_INTERACTIONS = "gjira.interactions";

    private final VectorizationPipeline vectorizationPipeline;
    private final KafkaTemplate<String, StandardInteractionFormat> kafkaTemplate;
    private final boolean kafkaEnabled;

    public IngestPublisher(
            VectorizationPipeline vectorizationPipeline,
            @Autowired(required = false) IngestPublisherProperties properties,
            @Autowired(required = false) KafkaTemplate<String, StandardInteractionFormat> kafkaTemplate
    ) {
        this.vectorizationPipeline = vectorizationPipeline;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = properties != null && properties.isKafkaEnabled() && kafkaTemplate != null;
    }

    public void publish(StandardInteractionFormat format) {
        if (kafkaEnabled) {
            try {
                kafkaTemplate.send(TOPIC_INTERACTIONS, format.tenantId().toString(), format);
            } catch (Exception e) {
                log.warn("Kafka send failed, falling back to direct vectorization: {}", e.getMessage());
                vectorizationPipeline.process(format);
            }
        } else {
            vectorizationPipeline.process(format);
        }
    }
}
