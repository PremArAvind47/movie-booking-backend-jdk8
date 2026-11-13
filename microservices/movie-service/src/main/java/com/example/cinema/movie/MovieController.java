package com.example.cinema.movie;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** PUBLIC API (the gateway exposes it): browse movies. */
@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {

    private final SeatInventory inventory;

    public MovieController(SeatInventory inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public List<MovieView> list() {
        return inventory.list();
    }

    @GetMapping("/{id}")
    public MovieView get(@PathVariable Long id) {
        return inventory.get(id);
    }
}
