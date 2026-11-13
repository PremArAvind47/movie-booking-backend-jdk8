package com.example.cinema.movie;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** INTERNAL API (only booking-service calls it; the gateway does NOT route to it). */
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final SeatInventory inventory;

    public ReservationController(SeatInventory inventory) {
        this.inventory = inventory;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Reservation reserve(@Valid @RequestBody ReserveRequest request) {
        return inventory.reserve(request.reservationKey(), request.movieId(), request.seats());
    }

    @DeleteMapping("/{reservationKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable String reservationKey) {
        inventory.release(reservationKey);
    }
}
