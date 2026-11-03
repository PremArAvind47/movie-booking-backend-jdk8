package com.example.moviebooking.controller;

import com.example.moviebooking.cache.AnswerCache;
import com.example.moviebooking.dto.GroundedAnswer;
import com.example.moviebooking.dto.MovieInfo;
import com.example.moviebooking.dto.TicketInfo;
import com.example.moviebooking.guard.InputGuard;
import com.example.moviebooking.tools.MovieTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import reactor.core.publisher.Flux;

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
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final MovieTools movieTools;
    private final String visionModel;
    private final InputGuard inputGuard;
    private final AnswerCache answerCache;

    public AiController(ChatClient.Builder builder, EmbeddingModel embeddingModel, VectorStore vectorStore,
                        @Value("classpath:movies.txt") Resource moviesFile,
                        @Value("${rag.top-k}") int topK,
                        @Value("${rag.min-score}") double minScore,
                        ChatMemory chatMemory,
                        MovieTools movieTools,
                        @Value("${vision.model}") String visionModel,
                        InputGuard inputGuard,
                        AnswerCache answerCache) {
        this.chatClient = builder.build();
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.moviesFile = moviesFile;
        this.topK = topK;
        this.minScore = minScore;
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        this.movieTools = movieTools;
        this.visionModel = visionModel;
        this.inputGuard = inputGuard;
        this.answerCache = answerCache;
    }

    // ===== Parts 1-2: ask Llama, with a rule (system) and extra info (context) =====

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

    // ===== Part 3: structured output (AI answer -> Java object) =====

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

    // ===== Part 4: embeddings (text -> numbers) =====

    @GetMapping("/ai/embed")
    public Map<String, Object> embed(@RequestParam String text) {
        float[] vector = embeddingModel.embed(text);
        return Map.of(
                "text", text,
                "dimensions", vector.length,
                "firstFiveValues", Arrays.copyOfRange(vector, 0, 5)
        );
    }

    // ===== Part 5: vector store search (search by meaning) =====

    @GetMapping("/ai/search")
    public List<String> search(@RequestParam String query) {
        List<Document> results = vectorStore.similaritySearch(query);
        return results.stream().map(Document::getText).toList();
    }

    // ===== Part 6: read the file, cut it into chunks (and Part 8: store them at startup) =====

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

    // runs once, automatically, when the app has finished starting
    @EventListener(ApplicationReadyEvent.class)
    public void loadFileOnStartup() {
        List<Document> chunks = splitFileIntoChunks();
        vectorStore.add(chunks);   // each chunk is turned into numbers and stored
        log.info("Loaded {} chunks from {} into the vector store", chunks.size(), moviesFile.getFilename());
    }

    // ===== Parts 7-8, 13 and 17: RAG - answer only from our file, CHECK the answer, and cache it =====

    @GetMapping("/ai/ask")
    public String ask(@RequestParam String question, @RequestParam(required = false) String conversationId) {
        inputGuard.check(question);   // Part 14: check the text before anything else

        // Part 17: same question asked before? give the saved answer, no search and no model call.
        // (a conversation uses memory, so its answers are not shared through the cache)
        if (conversationId == null) {
            String cached = answerCache.get(question);
            if (cached != null) {
                log.info("CACHE hit: answer taken from the cache, no search and no model call");
                return cached;
            }
        }

        // 1. find chunks: only from our file, only the best few, only if close enough
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(minScore)
                .filterExpression("source == '" + moviesFile.getFilename() + "'")
                .build();
        long searchStart = System.currentTimeMillis();
        List<Document> found = vectorStore.similaritySearch(request);
        log.info("TIMING search: {} ms, {} chunk(s) found", System.currentTimeMillis() - searchStart, found.size());
        found.forEach(doc -> log.info("RAG chunk score={} text={}", doc.getScore(), doc.getText()));

        // 2. nothing close enough -> say "I don't know" (do not ask Llama)
        if (found.isEmpty()) {
            return "I don't know. The file has no information about that.";
        }

        // 3. join their text into one block of information
        String information = found.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 4. ask Llama for an answer AND the exact sentence that proves it (evidence).
        //    The information is NOT saved in memory, only the question and the answer are.
        ChatClient.ChatClientRequestSpec prompt = chatClient
                .prompt()
                .system(system -> system
                        .text("Answer the question using only the information below. "
                                + "Copy the exact sentence from the information that proves your answer into \"evidence\". "
                                + "Write a short answer in \"answer\". When \"found\" is true, \"answer\" must never be empty. "
                                + "If the information does not contain the answer, set \"found\" to false "
                                + "and leave \"answer\" and \"evidence\" empty. "
                                + "Keep the answer short.\n\nInformation:\n{information}")
                        .param("information", information))
                .user(question);

        // 5. conversationId given -> remember this conversation
        if (conversationId != null) {
            prompt = prompt
                    .advisors(memoryAdvisor)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
        }
        long modelStart = System.currentTimeMillis();
        GroundedAnswer result = prompt.call().entity(GroundedAnswer.class);
        log.info("TIMING model: {} ms", System.currentTimeMillis() - modelStart);

        // 6. grounding check in Java: is the evidence REALLY inside the chunks we found?
        //    If not, Llama made it up, so we do not trust the answer.
        if (result == null || !result.found() || isBlank(result.evidence())) {
            log.warn("Answer rejected, the model said not found or gave no evidence: {}", result);
            return NOT_ENOUGH_INFORMATION;
        }
        String evidence = normalize(result.evidence());
        Document source = found.stream()
                .filter(doc -> normalize(doc.getText()).contains(evidence))
                .findFirst()
                .orElse(null);
        if (source == null) {
            log.warn("Answer rejected, evidence not found in the chunks: {}", result.evidence());
            return NOT_ENOUGH_INFORMATION;
        }

        // 7. grounded answer + where it came from.
        //    If the model left the answer empty, the evidence sentence (from our file) is the answer.
        String answer = result.answer();
        if (isBlank(answer)) {
            log.info("The model left the answer empty, using the evidence sentence instead");
            answer = result.evidence();
        }
        String finalAnswer = answer + " (source: " + moviesFile.getFilename()
                + ", chunk " + source.getMetadata().get("chunkNumber") + ")";
        if (conversationId == null) {
            answerCache.put(question, finalAnswer);   // Part 17: only good, checked answers are saved
        }
        return finalAnswer;
    }

    private static final String NOT_ENOUGH_INFORMATION = "I don't have enough information to answer that.";

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    // lower case + single spaces, so small differences in spacing do not matter
    private static String normalize(String text) {
        return text.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    // ===== Part 9: memory (remember the conversation) =====

    @GetMapping("/ai/chat")
    public String chat(@RequestParam String conversationId, @RequestParam String message) {
        inputGuard.check(message);   // Part 14
        return chatClient
                .prompt()
                .advisors(memoryAdvisor)                                        // turn memory on
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))   // whose memory
                .user(message)
                .call()
                .content();
    }

    // ===== Part 10: tool calling (Llama asks, our Java runs) =====

    @GetMapping("/ai/tools")
    public String tools(@RequestParam String question,
                        @RequestHeader(value = "X-Role", defaultValue = "GUEST") String role) {
        inputGuard.check(question);   // Part 14
        return chatClient
                .prompt()
                .system("You are a movie booking assistant. For seats and prices, always use the tools. "
                        + "Never guess numbers.")
                .tools(movieTools)          // the Java methods Llama may ask for
                .toolContext(Map.of("role", role))   // Part 14: who is asking (Java decides, not the AI)
                .user(question)
                .call()
                .content();
    }

    // ===== Part 11: agent (many tool calls, one after another) =====

    @GetMapping("/ai/agent")
    public String agent(@RequestParam String task,
                        @RequestHeader(value = "X-Role", defaultValue = "GUEST") String role) {
        inputGuard.check(task);   // Part 14
        return chatClient
                .prompt()
                .system("You are a movie booking agent. Solve the task step by step. "
                        + "Use the tools as many times as needed, one after another, "
                        + "and use each result to decide the next step. "
                        + "Answer only when you have all the facts. Never guess numbers.")
                .tools(movieTools)
                .toolContext(Map.of("role", role))   // Part 14
                .user(task)
                .call()
                .content();
    }

    // ===== Part 12: multimodal (read a picture) =====

    @PostMapping("/ai/read-ticket")
    public TicketInfo readTicket(@RequestParam("image") MultipartFile image) {
        return chatClient
                .prompt()
                .options(OllamaChatOptions.builder().model(visionModel))   // use the model that can see
                .user(user -> user
                        .text("This is a photo of a movie ticket. Read it and give the movie name, the seat and the price.")
                        .media(MimeTypeUtils.parseMimeType(image.getContentType()), image.getResource()))
                .call()
                .entity(TicketInfo.class);
    }


    // ===== Part 17: streaming (words appear while the model is still writing) =====

    @GetMapping(value = "/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String question) {
        inputGuard.check(question);
        return chatClient
                .prompt()
                .system("You are a helpful movie assistant. Keep the answer short.")
                .user(question)
                .stream()          // instead of .call(): pieces arrive one by one
                .content();
    }
}
