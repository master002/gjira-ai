package com.gjira.ingest.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dynamic_vectors", schema = "gjira")
public class DynamicVectorEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 255)
    private String sourceId;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "CLOB")
    private String chunkText;

    @Lob
    @Column(name = "embedding")
    private byte[] embedding;

    @Column(name = "ingested_at")
    private Instant ingestedAt;

    @Column(name = "event_ts", nullable = false)
    private Instant eventTs;

    @Column(name = "metadata", columnDefinition = "CLOB")
    private String metadata;

    public DynamicVectorEntity() {}

    public DynamicVectorEntity(String id, String tenantId, String sourceType, String sourceId,
                               String contentHash, String chunkText, float[] embedding,
                               Instant ingestedAt, Instant eventTs, String metadata) {
        this.id = id;
        this.tenantId = tenantId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.contentHash = contentHash;
        this.chunkText = chunkText;
        this.embedding = floatArrayToBytes(embedding);
        this.ingestedAt = ingestedAt;
        this.eventTs = eventTs;
        this.metadata = metadata;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (ingestedAt == null) ingestedAt = Instant.now();
    }

    private static byte[] floatArrayToBytes(float[] arr) {
        if (arr == null) return null;
        byte[] bytes = new byte[arr.length * 4];
        for (int i = 0; i < arr.length; i++) {
            int bits = Float.floatToIntBits(arr[i]);
            bytes[i * 4] = (byte) (bits >> 24);
            bytes[i * 4 + 1] = (byte) (bits >> 16);
            bytes[i * 4 + 2] = (byte) (bits >> 8);
            bytes[i * 4 + 3] = (byte) bits;
        }
        return bytes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }
    public byte[] getEmbedding() { return embedding; }
    public void setEmbedding(byte[] embedding) { this.embedding = embedding; }
    public Instant getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(Instant ingestedAt) { this.ingestedAt = ingestedAt; }
    public Instant getEventTs() { return eventTs; }
    public void setEventTs(Instant eventTs) { this.eventTs = eventTs; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
