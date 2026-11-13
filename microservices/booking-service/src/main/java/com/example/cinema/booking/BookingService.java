package com.example.cinema.booking;

import com.example.cinema.booking.client.MovieClient;
import com.example.cinema.booking.client.MovieDto;
import com.example.cinema.booking.client.PaymentClient;
import com.example.cinema.booking.client.PaymentRequest;
import com.example.cinema.booking.client.PaymentResult;
import com.example.cinema.booking.client.ReserveRequest;
import com.example.cinema.platform.ApiException;
import com.example.cinema.platform.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * ORCHESTRATION: this service is the boss of a booking. It calls the other services in order and
 * decides what to undo when a step fails (a small "saga"):
 *
 *   1. get the movie (price)            nothing to undo yet
 *   2. reserve the seats                undo = release the seats
 *   3. charge the payment               if declined or not available: release the seats (compensation)
 *   4. save the booking as CONFIRMED
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final MovieClient movieClient;
    private final PaymentClient paymentClient;
    private final BookingRepository repository;
    private final BookingRules rules;

    public BookingService(MovieClient movieClient, PaymentClient paymentClient,
                          BookingRepository repository, BookingRules rules) {
        this.movieClient = movieClient;
        this.paymentClient = paymentClient;
        this.repository = repository;
        this.rules = rules;
    }

    public Booking book(CreateBookingRequest request) {
        if (request.seats() > rules.maxSeatsPerBooking()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_SEATS",
                    "You can book at most " + rules.maxSeatsPerBooking() + " seats at once");
        }
        String bookingId = UUID.randomUUID().toString();
        // read here, in the request thread: the circuit breaker calls run in another thread
        String requestId = Objects.toString(MDC.get(RequestIdFilter.MDC_KEY), "none");

        // 1. movie and price (a 404 or an outage ends the booking here, nothing to undo)
        MovieDto movie = movieClient.getMovie(request.movieId(), requestId);
        int amount = movie.price() * request.seats();

        // 2. reserve the seats (409 NOT_ENOUGH_SEATS ends the booking here, nothing to undo)
        movieClient.reserve(new ReserveRequest(bookingId, movie.id(), request.seats()), requestId);

        Booking booking = new Booking(bookingId, movie.id(), movie.title(), request.seats(), amount,
                BookingStatus.FAILED, "in progress", Instant.now());

        // 3. charge the payment
        PaymentResult payment;
        try {
            payment = paymentClient.pay(new PaymentRequest(bookingId, amount), requestId);
        } catch (ApiException e) {
            fail(booking, BookingStatus.FAILED, e.getCode(), requestId);
            throw e;
        }
        if (!payment.approved()) {
            fail(booking, BookingStatus.PAYMENT_DECLINED, "Payment declined: " + payment.message(), requestId);
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_DECLINED",
                    "Payment declined (" + payment.message() + "). Your seats were released. Booking id: " + bookingId);
        }

        // 4. done
        Booking confirmed = booking.withStatus(BookingStatus.CONFIRMED, "Paid, payment id " + payment.paymentId());
        repository.save(confirmed);
        log.info("Booking {} confirmed: {} x {} = {}", bookingId, movie.title(), request.seats(), amount);
        return confirmed;
    }

    public Booking get(String id) {
        return repository.find(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "No booking with id " + id));
    }

    public List<Booking> list() {
        return repository.all();
    }

    /** COMPENSATION: give the seats back. If even that fails, mark the booking so someone can repair it. */
    private void fail(Booking booking, BookingStatus status, String reason, String requestId) {
        try {
            movieClient.release(booking.id(), requestId);
            repository.save(booking.withStatus(status, reason + ". Seats released."));
        } catch (RuntimeException e) {
            log.error("COMPENSATION FAILED for booking {}: the seats are still held. Needs a repair.", booking.id(), e);
            repository.save(booking.withStatus(BookingStatus.NEEDS_ATTENTION,
                    reason + ". The seats could NOT be released: needs attention."));
        }
    }
}
