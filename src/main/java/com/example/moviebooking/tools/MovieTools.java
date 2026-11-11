package com.example.moviebooking.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Java methods that Llama is allowed to ask for. Llama never runs them itself: it only asks,
// and our app runs the method and gives the result back to Llama.
@Component
public class MovieTools {

    private static final Logger log = LoggerFactory.getLogger(MovieTools.class);

    private final VectorStore vectorStore;

    public MovieTools(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // sample data only Java knows (Llama cannot know these numbers)
    private static final Map<String, Integer> FREE_SEATS = Map.of("avengers", 42, "batman", 7, "dune", 120);
    private static final Map<String, Integer> PRICES = Map.of("avengers", 250, "batman", 200, "dune", 300);

    // private sample data: only an ADMIN may read it
    private static final Map<Integer, String> CUSTOMERS = Map.of(
            101, "Ravi Kumar, phone 90000 00101, email ravi@example.com",
            102, "Anu Devi, phone 90000 00102, email anu@example.com");

    // The role comes from our Java code (ToolContext), never from the AI.
    // The AI can only ASK for this tool. Java decides if the user is allowed.
    @Tool(description = "Get the private details (name, phone, email) of a customer by customer id")
    public String getCustomerDetails(@ToolParam(description = "The customer id the user gave, for example 101. Do not invent one.") int customerId,
                                     ToolContext toolContext) {
        Object role = toolContext.getContext().get("role");
        log.info("TOOL getCustomerDetails called with customerId={} by role={}", customerId, role);
        if (!"ADMIN".equals(role)) {
            log.warn("ACCESS DENIED: role={} tried to read customer {}", role, customerId);
            return "Access denied: only an admin can see customer details.";
        }
        String details = CUSTOMERS.get(customerId);
        return details != null ? details : "Customer not found.";
    }

    @Tool(description = "Get the list of all movies that are showing now")
    public String getMovieList() {
        log.info("TOOL getMovieList called");
        return "Avengers, Batman, Dune";
    }

    @Tool(description = "Get how many seats are free right now for a movie")
    public String getFreeSeats(@ToolParam(description = "The movie name, for example Avengers") String movieName) {
        log.info("TOOL getFreeSeats called with movieName={}", movieName);
        Integer seats = FREE_SEATS.get(movieName.trim().toLowerCase());
        if (seats == null) {
            return "Movie not found. Available movies: Avengers, Batman, Dune.";
        }
        return movieName + " has " + seats + " free seats.";
    }

    @Tool(description = "Get the ticket price in rupees for a movie")
    public String getTicketPrice(@ToolParam(description = "The movie name, for example Dune") String movieName) {
        log.info("TOOL getTicketPrice called with movieName={}", movieName);
        Integer price = PRICES.get(movieName.trim().toLowerCase());
        if (price == null) {
            return "Movie not found. Available movies: Avengers, Batman, Dune.";
        }
        return "A ticket for " + movieName + " costs " + price + " rupees.";
    }

    // The model decides when the user wants something remembered. Java saves it in the vector store
    // (label source = "memory"). The store is in RAM, so it is lost on restart.
    @Tool(description = "Save a fact the user wants you to remember, for example 'my favourite colour is green'. "
            + "Use it only when the user asks to remember, keep or note something.")
    public String rememberFact(@ToolParam(description = "The fact to remember, as one short sentence") String fact) {
        log.info("TOOL rememberFact called with fact={}", fact);
        if (fact == null || fact.isBlank()) {
            return "Nothing to save.";
        }
        vectorStore.add(List.of(new Document(fact.trim(), Map.of("source", "memory"))));
        return "Saved: " + fact.trim();
    }
}
