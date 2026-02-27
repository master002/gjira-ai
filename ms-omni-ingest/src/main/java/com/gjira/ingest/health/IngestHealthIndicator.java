package com.gjira.ingest.health;

import com.gjira.ingest.repository.DynamicVectorRepository;
import com.gjira.ingest.repository.StaticLibraryVectorRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class IngestHealthIndicator implements HealthIndicator {

    private final DynamicVectorRepository dynamicRepo;
    private final StaticLibraryVectorRepository staticRepo;

    public IngestHealthIndicator(DynamicVectorRepository dynamicRepo,
                                 StaticLibraryVectorRepository staticRepo) {
        this.dynamicRepo = dynamicRepo;
        this.staticRepo = staticRepo;
    }

    @Override
    public Health health() {
        try {
            long dynamicCount = dynamicRepo.count();
            long staticCount = staticRepo.count();
            return Health.up()
                    .withDetail("dynamic_vectors", dynamicCount)
                    .withDetail("static_vectors", staticCount)
                    .build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
