package com.gjira.ingest.naming;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class StaticFileNamingUtil {

    private static final String DEFAULT_INITIALS_PAD = "XXX";

    private StaticFileNamingUtil() {
    }

    public static String buildStaticFileName(String tenantInitials,
                                             LocalDate latestDate,
                                             String extension) {
        if (latestDate == null) {
            throw new IllegalArgumentException("latestDate must not be null");
        }
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("extension must not be null or blank");
        }

        String normalizedInitials = normalizeInitials(tenantInitials);

        String datePart = latestDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format("%s_%s_STATIC.%s", datePart, normalizedInitials, extension);
    }

    private static String normalizeInitials(String tenantInitials) {
        String initials = tenantInitials == null
                ? DEFAULT_INITIALS_PAD
                : tenantInitials.trim().toUpperCase(Locale.ROOT);

        if (initials.length() < 3) {
            initials = (initials + DEFAULT_INITIALS_PAD).substring(0, 3);
        } else if (initials.length() > 3) {
            initials = initials.substring(0, 3);
        }
        return initials;
    }
}

