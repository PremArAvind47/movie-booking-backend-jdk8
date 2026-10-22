package com.example.moviebooking.controller;

import com.example.moviebooking.dto.MovieInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final Resource moviesFile;

    public AiController(ChatClient.Builder builder, EmbeddingModel embeddingModel, VectorStore vectorStore,
                        @Value("classpath:movies.txt") Resource moviesFile) {
        this.chatClient = builder.build();
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.moviesFile = moviesFile;
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

    @GetMapping("/ai/read-file")
    public Map<String, Object> readFile() {
        List<Document> documents = new TextReader(moviesFile).get();
        return Map.of(
                "documents", documents.size(),
                "characters", documents.get(0).getText().length()
        );
    }

    @GetMapping("/ai/chunks")
    public Map<String, Object> chunks() {
        List<Document> chunks = splitFileIntoChunks();

        return Map.of(
                "chunkCount", chunks.size(),
                "chunks", chunks.stream().map(Document::getText).toList()
        );
    }

    @GetMapping("/ai/load-file")
    public Map<String, Object> loadFile() {
        List<Document> chunks = splitFileIntoChunks();
        vectorStore.add(chunks);   // each chunk is turned into numbers and stored
        return Map.of("storedChunks", chunks.size());
    }

    private List<Document> splitFileIntoChunks() {
        List<Document> documents = new TextReader(moviesFile).get();
        String wholeText = documents.get(0).getText();

        // cut at every blank line (one paragraph = one chunk)
        String[] pieces = wholeText.split("\\R\\s*\\R");

        List<Document> chunks = new ArrayList<>();
        // metadata = extra label on the chunk (which file it came from, which number it is)
        Map<String, Object> metadata = new HashMap<>(documents.get(0).getMetadata());
        for (String piece : pieces) {
            if (!piece.isBlank()) {
                metadata.put("chunkNumber", chunks.size() + 1);
                chunks.add(new Document(piece.trim(), metadata));
            }
        }
        return chunks;
    }

    @GetMapping("/ai/chunks-by-size")
    public Map<String, Object> chunksBySize(@RequestParam int size, @RequestParam int overlap) {
        if (size <= 0 || overlap < 0 || overlap >= size) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "size must be more than 0, and overlap must be 0 or more and less than size");
        }
        String wholeText = new TextReader(moviesFile).get().get(0).getText();
        List<String> chunks = splitBySize(wholeText, size, overlap);

        return Map.of(
                "size", size,
                "overlap", overlap,
                "chunkCount", chunks.size(),
                "chunks", chunks
        );
    }

    // cut every "size" characters, but start each new chunk "overlap" characters earlier
    private List<String> splitBySize(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + size, text.length());
            chunks.add(text.substring(start, end).trim());
            if (end == text.length()) {
                break;
            }
            start = end - overlap;
        }
        return chunks;
    }

    @GetMapping("/ai/search-detail")
    public List<Map<String, Object>> searchDetail(@RequestParam String query) {
        List<Document> results = vectorStore.similaritySearch(query);
        return results.stream()
                .map(doc -> Map.of(
                        "text", doc.getText(),
                        "score", doc.getScore() == null ? 0.0 : doc.getScore(),
                        "metadata", doc.getMetadata()))
                .toList();
    }
}
