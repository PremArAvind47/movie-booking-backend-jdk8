package com.example.moviebooking.cost;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Part 18: turns token counts into money.
// Our local model is free. This answers: "what would the same call cost on a paid cloud model?"
// The prices come from application.properties (example numbers, check the real price list).
@Component
public class CostEstimator {

    private final double inputPerMillion;
    private final double outputPerMillion;

    public CostEstimator(@Value("${cost.input-per-million-tokens}") double inputPerMillion,
                         @Value("${cost.output-per-million-tokens}") double outputPerMillion) {
        this.inputPerMillion = inputPerMillion;
        this.outputPerMillion = outputPerMillion;
    }

    // tokens in (question + information) and tokens out (answer) -> money in dollars
    public double estimate(int inputTokens, int outputTokens) {
        return inputTokens * inputPerMillion / 1_000_000.0
                + outputTokens * outputPerMillion / 1_000_000.0;
    }
}
