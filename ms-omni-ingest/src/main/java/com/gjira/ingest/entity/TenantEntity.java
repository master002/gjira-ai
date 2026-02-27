package com.gjira.ingest.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tenants", schema = "gjira")
public class TenantEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "org_code", nullable = false, unique = true, length = 3)
    private String orgCode;

    @Column(name = "org_name", nullable = false)
    private String orgName;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "config", columnDefinition = "CLOB")
    private String config;

    public TenantEntity() {}

    public TenantEntity(String id, String orgCode, String orgName) {
        this.id = id;
        this.orgCode = orgCode;
        this.orgName = orgName;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
