package com.example.moviebooking.controller;

import com.example.moviebooking.dto.MovieInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final Resource moviesFile;
    private final int topK;
    private final double minScore;

    public AiController(ChatClient.Builder builder, EmbeddingModel embeddingModel, VectorStore vectorStore,
                        @Value("classpath:movies.txt") Resource moviesFile,
                        @Value("${rag.top-k}") int topK,
                        @Value("${rag.min-score}") double minScore) {
        this.chatClient = builder.build();
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.moviesFile = moviesFile;
        this.topK = topK;
        this.minScore = minScore;
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

    @GetMapping("/ai/ask")
    public String ask(@RequestParam String question) {
        // 1. find chunks: only from our file, only the best few, only if close enough
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(minScore)
                .filterExpression("source == '" + moviesFile.getFilename() + "'")
                .build();
        List<Document> found = vectorStore.similaritySearch(request);
        found.forEach(doc -> log.info("RAG chunk score={} text={}", doc.getScore(), doc.getText()));

        // 2. nothing close enough -> say "I don't know" (do not ask Llama)
        if (found.isEmpty()) {
            return "I don't know. The file has no information about that.";
        }

        // 3. join their text into one block of information
        String information = found.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 4. give the information + question to Llama
        return chatClient
                .prompt()
                .system("Answer the question using only the information given. "
                        + "If the answer is not in the information, say \"I don't know\". Keep the answer short.")
                .user("Information:\n" + information + "\n\nQuestion: " + question)
                .call()
                .content();
    }
}
