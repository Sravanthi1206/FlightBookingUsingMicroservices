package com.flightapp.service;

import com.flightapp.model.Booking;
import com.flightapp.repository.BookingRepository;
import com.flightapp.client.FlightClient;
import com.flightapp.dto.BookingMessage;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class BookingServiceTest {

    @Test
    public void createBookingRejectsPastFlight() {
        BookingRepository repo = Mockito.mock(BookingRepository.class);
        FlightClient client = Mockito.mock(FlightClient.class);
        KafkaTemplate<String, BookingMessage> k = Mockito.mock(KafkaTemplate.class);

        BookingService svc = new BookingService(repo, client, k, "topic", 24L);

        // flight in past
        FlightClient.FlightInfo fi = new FlightClient.FlightInfo(
                "f1", "BLR", "DEL", 100, 100, LocalDate.now().minusDays(1), Instant.now().minusSeconds(3600)
        );
        when(client.get("f1")).thenReturn(ResponseEntity.ok(fi));

        Booking req = new Booking(null, "f1", "u@example.com", 1, null, Instant.now(), null);

        assertThrows(ResponseStatusException.class, () -> svc.createBooking(req));
    }

    @Test
    public void cancelBookingRejectsWithinWindow() {
        BookingRepository repo = Mockito.mock(BookingRepository.class);
        FlightClient client = Mockito.mock(FlightClient.class);
        KafkaTemplate<String, BookingMessage> k = Mockito.mock(KafkaTemplate.class);

        Booking existing = new Booking("b1", "f1", "u@example.com", 2, "CONFIRMED", Instant.now(), null);
        when(repo.findById("b1")).thenReturn(java.util.Optional.of(existing));

        // flight departs in 1 hour
        FlightClient.FlightInfo fi = new FlightClient.FlightInfo(
                "f1", "BLR", "DEL", 100, 100, LocalDate.now(), Instant.now().plusSeconds(3600)
        );
        when(client.get("f1")).thenReturn(ResponseEntity.ok(fi));

        BookingService svc = new BookingService(repo, client, k, "topic", 24L);

        assertThrows(ResponseStatusException.class, () -> svc.cancelBooking("b1"));
    }

    @Test
    public void cancelBookingSucceedsOutsideWindow() {
        BookingRepository repo = Mockito.mock(BookingRepository.class);
        FlightClient client = Mockito.mock(FlightClient.class);
        KafkaTemplate<String, BookingMessage> k = Mockito.mock(KafkaTemplate.class);

        Booking existing = new Booking("b2", "f2", "u@example.com", 1, "CONFIRMED", Instant.now(), null);
        when(repo.findById("b2")).thenReturn(java.util.Optional.of(existing));

        // flight departs in 48 hours
        FlightClient.FlightInfo fi = new FlightClient.FlightInfo(
                "f2", "BLR", "DEL", 100, 100, LocalDate.now(), Instant.now().plus(Duration.ofHours(48))
        );
        when(client.get("f2")).thenReturn(ResponseEntity.ok(fi));

        // release call should return ok
        when(client.release(eq("f2"), any())).thenReturn(ResponseEntity.ok(null));

        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        BookingService svc = new BookingService(repo, client, k, "topic", 24L);

        Booking updated = svc.cancelBooking("b2");

        assertEquals("CANCELLED", updated.getStatus());
        verify(k).send(eq("booking.cancelled"), eq("b2"), any(BookingMessage.class));
    }
}
