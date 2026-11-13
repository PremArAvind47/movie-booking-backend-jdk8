package com.example.cinema.movie;

import com.example.cinema.platform.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Fast tests. No Spring, no network.
class SeatInventoryTest {

    private final SeatInventory inventory = new SeatInventory();

    @Test
    void reserveTakesSeats() {
        inventory.reserve("b1", 2L, 3);
        assertEquals(4, inventory.get(2L).freeSeats());   // Batman starts with 7
    }

    @Test
    void sameKeyTwiceTakesSeatsOnlyOnce() {
        inventory.reserve("b1", 2L, 3);
        inventory.reserve("b1", 2L, 3);
        assertEquals(4, inventory.get(2L).freeSeats());
    }

    @Test
    void notEnoughSeatsGivesConflict() {
        ApiException e = assertThrows(ApiException.class, () -> inventory.reserve("b1", 2L, 8));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("NOT_ENOUGH_SEATS", e.getCode());
        assertEquals(7, inventory.get(2L).freeSeats());   // nothing was taken
    }

    @Test
    void releaseGivesSeatsBackAndIsSafeToRepeat() {
        inventory.reserve("b1", 2L, 3);
        inventory.release("b1");
        inventory.release("b1");
        assertEquals(7, inventory.get(2L).freeSeats());
    }

    @Test
    void unknownMovieGivesNotFound() {
        ApiException e = assertThrows(ApiException.class, () -> inventory.get(99L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }
}
