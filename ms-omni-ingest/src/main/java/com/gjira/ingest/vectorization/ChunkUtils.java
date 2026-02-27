package com.gjira.ingest.vectorization;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ChunkUtils {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");

    private ChunkUtils() {}

    public static List<String> chunk(String text, int maxChunkSize) {
        if (text == null || text.isBlank()) return List.of();

        List<String> result = new ArrayList<>();
        String[] sentences = SENTENCE_BOUNDARY.split(text);
        StringBuilder current = new StringBuilder();

        for (String s : sentences) {
            if (current.length() + s.length() + 1 > maxChunkSize && current.length() > 0) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (current.length() > 0) current.append(" ");
            current.append(s);
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }
}
