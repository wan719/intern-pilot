package com.internpilot.util;

import java.util.ArrayList;
import java.util.List;

public class TextChunkUtils {

    private static final int MAX_CHUNK_LENGTH = 800;
    private static final int MIN_CHUNK_LENGTH = 100;

    public static List<String> splitToChunks(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        String[] paragraphs = content.split("\\n\\s*\\n");
        List<String> chunks = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String clean = paragraph.trim();

            if (clean.isBlank()) {
                continue;
            }

            if (current.length() + clean.length() > MAX_CHUNK_LENGTH) {
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current.setLength(0);
                }

                if (clean.length() > MAX_CHUNK_LENGTH) {
                    chunks.addAll(splitLongText(clean));
                } else {
                    current.append(clean).append("\n");
                }
            } else {
                current.append(clean).append("\n");
            }
        }

        if (current.length() >= MIN_CHUNK_LENGTH) {
            chunks.add(current.toString().trim());
        } else if (current.length() > 0 && !chunks.isEmpty()) {
            int lastIndex = chunks.size() - 1;
            chunks.set(lastIndex, chunks.get(lastIndex) + "\n" + current.toString().trim());
        } else if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    private static List<String> splitLongText(String text) {
        List<String> result = new ArrayList<>();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, text.length());
            result.add(text.substring(start, end));
            start = end;
        }

        return result;
    }
}