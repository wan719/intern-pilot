package com.internpilot.util;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "文本分块工具类，提供了将长文本内容分割成多个较小的块的方法，以便在处理AI模型输入时能够更好地控制每个输入块的长度，确保符合模型的输入限制并提高处理效率")//这个注解用于Swagger API文档生成，提供了对该类的描述信息
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