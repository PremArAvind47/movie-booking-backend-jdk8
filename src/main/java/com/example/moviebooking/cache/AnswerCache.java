package com.example.moviebooking.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Part 17: remembers answers, so the same question does not go to the slow model again.
// It lives in the app's memory (lost on restart) and keeps only the newest "max-entries" answers.
@Component
public class AnswerCache {

    private final int maxEntries;
    private final Map<String, String> answers;

    public AnswerCache(@Value("${cache.max-entries}") int maxEntries) {
        this.maxEntries = maxEntries;
        // "true" = the entry used longest ago is the one removed when the cache is full
        this.answers = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > AnswerCache.this.maxEntries;
            }
        });
    }

    public String get(String question) {
        return answers.get(normalize(question));
    }

    public void put(String question, String answer) {
        answers.put(normalize(question), answer);
    }

    public void clear() {
        answers.clear();
    }

    public int size() {
        return answers.size();
    }

    // "Who directed  Inception?" and "who directed inception?" are the same question
    private static String normalize(String question) {
        return question.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}
