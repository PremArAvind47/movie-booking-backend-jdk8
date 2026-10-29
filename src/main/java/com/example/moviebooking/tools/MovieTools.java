package com.example.moviebooking.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

// Java methods that Llama is allowed to ask for. Llama never runs them itself: it only asks,
// and our app runs the method and gives the result back to Llama.
@Component
public class MovieTools {

    private static final Logger log = LoggerFactory.getLogger(MovieTools.class);

    // sample data only Java knows (Llama cannot know these numbers)
    private static final Map<String, Integer> FREE_SEATS = Map.of("avengers", 42, "batman", 7, "dune", 120);
    private static final Map<String, Integer> PRICES = Map.of("avengers", 250, "batman", 200, "dune", 300);

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
}
