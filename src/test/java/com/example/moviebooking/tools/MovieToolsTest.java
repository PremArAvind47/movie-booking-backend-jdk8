package com.example.moviebooking.tools;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Fast tests for our tools. They do not need Ollama or the AI.
class MovieToolsTest {

    private final MovieTools tools = new MovieTools();

    @Test
    void freeSeatsForBatmanIsSeven() {
        assertEquals("Batman has 7 free seats.", tools.getFreeSeats("Batman"));
    }

    @Test
    void movieNameIsNotCaseSensitive() {
        assertTrue(tools.getFreeSeats("  DUNE ").contains("120"));
    }

    @Test
    void unknownMovieSaysNotFound() {
        assertTrue(tools.getFreeSeats("Titanic").startsWith("Movie not found"));
    }

    @Test
    void ticketPriceForDuneIs300() {
        assertTrue(tools.getTicketPrice("Dune").contains("300"));
    }

    @Test
    void movieListHasAllThreeMovies() {
        assertEquals("Avengers, Batman, Dune", tools.getMovieList());
    }

    @Test
    void adminCanSeeCustomerDetails() {
        String result = tools.getCustomerDetails(101, new ToolContext(Map.of("role", "ADMIN")));
        assertTrue(result.contains("Ravi Kumar"));
    }

    @Test
    void guestIsDenied() {
        String result = tools.getCustomerDetails(101, new ToolContext(Map.of("role", "GUEST")));
        assertTrue(result.startsWith("Access denied"));
        assertTrue(!result.contains("ravi@example.com"));
    }

    @Test
    void missingRoleIsDenied() {
        String result = tools.getCustomerDetails(101, new ToolContext(Map.of()));
        assertTrue(result.startsWith("Access denied"));
    }

    @Test
    void adminAskingForUnknownCustomerGetsNotFound() {
        assertEquals("Customer not found.", tools.getCustomerDetails(999, new ToolContext(Map.of("role", "ADMIN"))));
    }
}
