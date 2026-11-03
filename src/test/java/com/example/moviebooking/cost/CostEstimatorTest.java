package com.example.moviebooking.cost;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Fast tests for the cost maths. They do not need Ollama.
class CostEstimatorTest {

    @Test
    void oneMillionInputTokensCostsTheInputPrice() {
        assertEquals(3.0, new CostEstimator(3.0, 15.0).estimate(1_000_000, 0), 0.0001);
    }

    @Test
    void outputTokensUseTheOutputPrice() {
        assertEquals(15.0, new CostEstimator(3.0, 15.0).estimate(0, 1_000_000), 0.0001);
    }

    @Test
    void inputAndOutputAreAdded() {
        // 1000 tokens in (0.003) + 500 tokens out (0.0075)
        assertEquals(0.0105, new CostEstimator(3.0, 15.0).estimate(1000, 500), 0.0000001);
    }

    @Test
    void zeroTokensCostNothing() {
        assertEquals(0.0, new CostEstimator(3.0, 15.0).estimate(0, 0), 0.0);
    }
}
