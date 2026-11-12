package com.internpilot.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkUtilsTest {

    @Test
    void splitToChunks_shouldReturnEmpty_whenContentBlank() {
        List<String> chunks = TextChunkUtils.splitToChunks("   ");

        assertThat(chunks).isEmpty();
    }

    @Test
    void splitToChunks_shouldSplitParagraphs() {
        String content = """
                Java后端岗位要求掌握Java基础、Spring Boot、MySQL。

                Redis是常见缓存中间件，需要理解缓存穿透、击穿和雪崩。

                Spring Security常用于认证和授权，JWT项目中通常会自定义过滤器。
                """;

        List<String> chunks = TextChunkUtils.splitToChunks(content);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0)).contains("Java后端");
    }
}