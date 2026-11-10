package com.example.moviebooking.tools;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// Fast tests for our tools. They do not need Ollama or the AI.
class MovieToolsTest {

    private final VectorStore store = mock(VectorStore.class);   // a fake store, no Ollama needed
    private final MovieTools tools = new MovieTools(store);

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

    @Test
    @SuppressWarnings("unchecked")
    void rememberFactSavesTheFactWithSourceMemory() {
        String reply = tools.rememberFact("  my favourite colour is green ");

        ArgumentCaptor<List<Document>> saved = ArgumentCaptor.forClass(List.class);
        verify(store).add(saved.capture());
        Document doc = saved.getValue().get(0);
        assertEquals("my favourite colour is green", doc.getText());
        assertEquals("memory", doc.getMetadata().get("source"));
        assertEquals("Saved: my favourite colour is green", reply);
    }

    @Test
    void rememberFactWithBlankTextSavesNothing() {
        assertEquals("Nothing to save.", tools.rememberFact("   "));
        verify(store, never()).add(any(List.class));
    }
}
