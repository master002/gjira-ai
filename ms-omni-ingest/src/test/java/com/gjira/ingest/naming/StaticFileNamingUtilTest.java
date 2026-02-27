package com.gjira.ingest.naming;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaticFileNamingUtilTest {

    @Test
    void padsInitialsWhenShorterThanThreeCharacters() {
        LocalDate date = LocalDate.of(2025, 2, 27);

        String result1 = StaticFileNamingUtil.buildStaticFileName("a", date, "docx");
        String result2 = StaticFileNamingUtil.buildStaticFileName("ab", date, "docx");
        String result3 = StaticFileNamingUtil.buildStaticFileName("", date, "docx");
        String result4 = StaticFileNamingUtil.buildStaticFileName(null, date, "docx");

        assertEquals("2025-02-27_AXX_STATIC.docx", result1);
        assertEquals("2025-02-27_ABX_STATIC.docx", result2);
        assertEquals("2025-02-27_XXX_STATIC.docx", result3);
        assertEquals("2025-02-27_XXX_STATIC.docx", result4);
    }

    @Test
    void truncatesInitialsWhenLongerThanThreeCharacters() {
        LocalDate date = LocalDate.of(2025, 2, 27);

        String result = StaticFileNamingUtil.buildStaticFileName("AcmeCorp", date, "docx");

        assertEquals("2025-02-27_ACM_STATIC.docx", result);
    }

    @Test
    void throwsOnNullDateOrBlankExtension() {
        assertThrows(IllegalArgumentException.class,
                () -> StaticFileNamingUtil.buildStaticFileName("ACM", null, "docx"));
        assertThrows(IllegalArgumentException.class,
                () -> StaticFileNamingUtil.buildStaticFileName("ACM", LocalDate.now(), null));
        assertThrows(IllegalArgumentException.class,
                () -> StaticFileNamingUtil.buildStaticFileName("ACM", LocalDate.now(), " "));
    }
}

