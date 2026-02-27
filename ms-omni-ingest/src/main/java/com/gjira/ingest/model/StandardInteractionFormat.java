package com.gjira.ingest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Normalized payload format for all ingested platform data.
 * Maps to Shadow DB dynamic_vectors / static_library_vectors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StandardInteractionFormat(
        @NotNull UUID id,
        @NotNull UUID tenantId,
        @NotNull StreamType streamType,
        @NotNull SourceType sourceType,
        @NotBlank @Size(max = 255) String sourceId,
        @NotNull Instant eventTs,
        @NotNull @Valid Content content,
        @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String contentHash
) {
    public record Content(
            @NotBlank String text,
            Map<String, Object> metadata
    ) {}

    public enum StreamType { DYNAMIC, STATIC }

    public enum SourceType { slack, teams, zoom, confluence, sharepoint, upload }
}
