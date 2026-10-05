package com.example.moviebooking.controller;

import com.example.moviebooking.dto.MovieInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public AiController(ChatClient.Builder builder, EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/ai")
    public String askAI(@RequestParam String question, @RequestParam(required = false) String context) {
        String userMessage = (context != null)
                ? "Context: " + context + "\n\nQuestion: " + question
                : question;

        log.debug("AI question: {}", question);
        try {
            return chatClient
                    .prompt()
                    .system("You are a helpful Java tutor. Answer in 2-3 short sentences, no long lists.")
                    .user(userMessage)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            log.error("AI call failed for question: {}", question, e);
            throw e;
        }
    }

    @GetMapping("/ai/movie-info")
    public MovieInfo getMovieInfo(@RequestParam String movieName) {
        log.debug("AI movie info for: {}", movieName);
        try {
            return chatClient
                    .prompt()
                    .user("Give info about the movie: " + movieName)
                    .call()
                    .entity(MovieInfo.class);
        } catch (RuntimeException e) {
            log.error("AI movie info failed for: {}", movieName, e);
            throw e;
        }
    }

    @GetMapping("/ai/embed")
    public Map<String, Object> embed(@RequestParam String text) {
        float[] vector = embeddingModel.embed(text);
        return Map.of(
                "text", text,
                "dimensions", vector.length,
                "firstFiveValues", Arrays.copyOfRange(vector, 0, 5)
        );
    }

    @GetMapping("/ai/search")
    public List<String> search(@RequestParam String query) {
        List<Document> results = vectorStore.similaritySearch(query);
        return results.stream().map(Document::getText).toList();
    }
}
