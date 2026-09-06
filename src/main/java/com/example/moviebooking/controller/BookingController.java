package com.example.moviebooking.controller;

import com.example.moviebooking.dto.BookingRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class BookingController {

    @PostMapping("/api/bookings")
    public String createBooking(@RequestBody BookingRequest request) {

        System.out.println(request.getMovieName());
        System.out.println(request.getDate());
        System.out.println(request.getTime());

        return "Booking created";
    }


    @GetMapping("/api/movies")
    public String getMovies() {

        return "["
                + "{\"name\":\"Avengers\",\"price\":250,\"image\":\"https://m.media-amazon.com/images/I/71eHZFw+GlL._AC_UF894,1000_QL80_.jpg\"},"
                + "{\"name\":\"Batman\",\"price\":200,\"image\":\"https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTwtrZsVABgAcPA2OktGdUqtuqx_ZhWj87meqc0eq4G4L39kfJDuB8ZzDsi&s=10\"},"
                + "{\"name\":\"Dune\",\"price\":300,\"image\":\"https://upload.wikimedia.org/wikipedia/en/thumb/8/8e/Dune_%282021_film%29.jpg/250px-Dune_%282021_film%29.jpg\"}"
                + "]";
    }






}
