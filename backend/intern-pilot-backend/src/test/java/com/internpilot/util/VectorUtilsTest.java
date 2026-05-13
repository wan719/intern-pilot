package com.internpilot.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VectorUtilsTest {

    @Test
    void cosineSimilarity_shouldReturnOne_whenVectorsSame() {
        List<Double> a = List.of(1.0, 0.0, 1.0);
        List<Double> b = List.of(1.0, 0.0, 1.0);

        double similarity = VectorUtils.cosineSimilarity(a, b);

        assertThat(similarity).isCloseTo(1.0, within(0.000001));
    }

    @Test
    void cosineSimilarity_shouldReturnZero_whenVectorEmpty() {
        double similarity = VectorUtils.cosineSimilarity(List.of(), List.of(1.0));

        assertThat(similarity).isEqualTo(0.0);
    }
}
