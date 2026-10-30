package com.example.moviebooking.dto;

// found = does the information contain the answer?
// evidence = the exact sentence from the information that proves the answer
public record GroundedAnswer(boolean found, String answer, String evidence) {
}
