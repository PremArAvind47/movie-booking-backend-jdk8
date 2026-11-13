package com.example.cinema.booking;

import com.example.cinema.booking.client.MovieClient;
import com.example.cinema.booking.client.MovieDto;
import com.example.cinema.booking.client.PaymentClient;
import com.example.cinema.booking.client.PaymentResult;
import com.example.cinema.booking.client.ReservationDto;
import com.example.cinema.platform.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests the ORCHESTRATION with fake clients: no network, no other service needed. */
class BookingServiceTest {

    private MovieClient movieClient;
    private PaymentClient paymentClient;
    private BookingRepository repository;
    private BookingService service;

    @BeforeEach
    void setUp() {
        movieClient = mock(MovieClient.class);
        paymentClient = mock(PaymentClient.class);
        repository = new BookingRepository();
        service = new BookingService(movieClient, paymentClient, repository, new BookingRules(6));
        when(movieClient.getMovie(any(), anyString())).thenReturn(new MovieDto(2L, "Batman", 200, 7));
        when(movieClient.reserve(any(), anyString())).thenReturn(new ReservationDto("k", 2L, 2));
    }

    @Test
    void happyPathConfirmsTheBooking() {
        when(paymentClient.pay(any(), anyString())).thenReturn(new PaymentResult("p1", "k", 400, "APPROVED", "Approved"));

        Booking booking = service.book(new CreateBookingRequest(2L, 2));

        assertEquals(BookingStatus.CONFIRMED, booking.status());
        assertEquals(400, booking.amount());                       // 200 x 2 seats
        verify(movieClient, never()).release(anyString(), anyString());
        assertEquals(1, repository.all().size());
    }

    @Test
    void declinedPaymentReleasesTheSeats() {
        when(paymentClient.pay(any(), anyString())).thenReturn(new PaymentResult("p1", "k", 400, "DECLINED", "Too big"));

        ApiException e = assertThrows(ApiException.class, () -> service.book(new CreateBookingRequest(2L, 2)));

        assertEquals(HttpStatus.PAYMENT_REQUIRED, e.getStatus());
        assertEquals("PAYMENT_DECLINED", e.getCode());
        verify(movieClient).release(anyString(), anyString());     // the undo step ran
        assertEquals(BookingStatus.PAYMENT_DECLINED, repository.all().get(0).status());
    }

    @Test
    void paymentServiceDownReleasesTheSeats() {
        when(paymentClient.pay(any(), anyString())).thenThrow(
                new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_SERVICE_UNAVAILABLE", "down"));

        ApiException e = assertThrows(ApiException.class, () -> service.book(new CreateBookingRequest(2L, 2)));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
        verify(movieClient).release(anyString(), anyString());
        assertEquals(BookingStatus.FAILED, repository.all().get(0).status());
    }

    @Test
    void failedUndoMarksTheBookingNeedsAttention() {
        when(paymentClient.pay(any(), anyString())).thenReturn(new PaymentResult("p1", "k", 400, "DECLINED", "No"));
        doThrow(new RuntimeException("movie-service is down")).when(movieClient).release(anyString(), anyString());

        assertThrows(ApiException.class, () -> service.book(new CreateBookingRequest(2L, 2)));

        Booking saved = repository.all().get(0);
        assertEquals(BookingStatus.NEEDS_ATTENTION, saved.status());
        assertTrue(saved.reason().contains("could NOT be released"));
    }

    @Test
    void notEnoughSeatsStopsBeforePaymentAndSavesNothing() {
        when(movieClient.reserve(any(), anyString())).thenThrow(
                new ApiException(HttpStatus.CONFLICT, "NOT_ENOUGH_SEATS", "Only 1 seat left"));

        ApiException e = assertThrows(ApiException.class, () -> service.book(new CreateBookingRequest(2L, 2)));

        assertEquals("NOT_ENOUGH_SEATS", e.getCode());
        verify(paymentClient, never()).pay(any(), anyString());    // nobody was charged
        verify(movieClient, never()).release(anyString(), anyString());
        assertTrue(repository.all().isEmpty());
    }

    @Test
    void tooManySeatsIsRejectedBeforeAnyCall() {
        ApiException e = assertThrows(ApiException.class, () -> service.book(new CreateBookingRequest(2L, 7)));

        assertEquals("TOO_MANY_SEATS", e.getCode());
        verify(movieClient, never()).getMovie(any(), anyString());
    }
}
