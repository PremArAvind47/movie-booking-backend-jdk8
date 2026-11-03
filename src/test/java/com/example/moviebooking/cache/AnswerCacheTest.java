package com.example.moviebooking.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// Fast tests for the answer cache. They do not need Ollama or the AI.
class AnswerCacheTest {

    @Test
    void remembersAnAnswer() {
        AnswerCache cache = new AnswerCache(10);
        cache.put("Who directed Inception?", "Nolan");
        assertEquals("Nolan", cache.get("Who directed Inception?"));
    }

    @Test
    void unknownQuestionGivesNull() {
        assertNull(new AnswerCache(10).get("Who directed Inception?"));
    }

    @Test
    void capitalsAndExtraSpacesDoNotMatter() {
        AnswerCache cache = new AnswerCache(10);
        cache.put("Who directed Inception?", "Nolan");
        assertEquals("Nolan", cache.get("  WHO   directed inception? "));
    }

    @Test
    void forgetsTheOldestWhenFull() {
        AnswerCache cache = new AnswerCache(2);
        cache.put("q1", "a1");
        cache.put("q2", "a2");
        cache.put("q3", "a3");           // full: q1 is the oldest, so it goes
        assertNull(cache.get("q1"));
        assertEquals("a2", cache.get("q2"));
        assertEquals("a3", cache.get("q3"));
        assertEquals(2, cache.size());
    }

    @Test
    void recentlyUsedAnswerIsKept() {
        AnswerCache cache = new AnswerCache(2);
        cache.put("q1", "a1");
        cache.put("q2", "a2");
        cache.get("q1");                 // q1 is used again, so q2 becomes the oldest
        cache.put("q3", "a3");
        assertEquals("a1", cache.get("q1"));
        assertNull(cache.get("q2"));
    }

    @Test
    void clearRemovesEverything() {
        AnswerCache cache = new AnswerCache(10);
        cache.put("q1", "a1");
        cache.clear();
        assertEquals(0, cache.size());
    }
}
