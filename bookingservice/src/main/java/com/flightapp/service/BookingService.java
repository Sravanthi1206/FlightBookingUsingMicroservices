package com.flightapp.service;

import com.flightapp.model.Booking;
import com.flightapp.repository.BookingRepository;
import com.flightapp.exception.NotFoundException;
import com.flightapp.client.FlightClient;
import com.flightapp.dto.BookingMessage;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.time.Instant;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepo;
    private final FlightClient flightClient;
    private final KafkaTemplate<String, BookingMessage> kafkaTemplate;
    private final String topic;

    public BookingService(BookingRepository bookingRepo,
                          FlightClient flightClient,
                          KafkaTemplate<String, BookingMessage> kafkaTemplate,
                          @Value("${booking.topic:booking.created}") String topic) {
        this.bookingRepo = bookingRepo;
        this.flightClient = flightClient;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public Booking getBooking(String id) {
        return bookingRepo.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    public Booking createBooking(Booking req) {
        String bookingId = UUID.randomUUID().toString();

        boolean reserved = reserveFlight(req.getFlightId(), bookingId, req.getSeats());
        if (!reserved) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to reserve seats");

        req.setId(bookingId);
        req.setStatus("CONFIRMED");
        req.setCreatedAt(Instant.now());
        Booking saved = bookingRepo.save(req);

        BookingMessage m = new BookingMessage(saved.getId(), saved.getUserEmail(), saved.getFlightId(), saved.getSeats());

        try {
            kafkaTemplate.send(topic, saved.getId(), m);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to publish booking event", e);
        }

        return saved;
    }

    public Booking cancelBooking(String id) {
        Booking b = bookingRepo.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        if ("CANCELLED".equalsIgnoreCase(b.getStatus())) return b;

        boolean released = releaseFlight(b.getFlightId(), b.getId(), b.getSeats());
        if (!released) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to contact flight service");

        b.setStatus("CANCELLED");
        Booking updated = bookingRepo.save(b);

        BookingMessage m = new BookingMessage(updated.getId(), updated.getUserEmail(), updated.getFlightId(), updated.getSeats());

        try {
            kafkaTemplate.send("booking.cancelled", updated.getId(), m);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to publish booking cancelled event", e);
        }

        return updated;
    }

    @CircuitBreaker(name = "flight", fallbackMethod = "reserveFallback")
    public boolean reserveFlight(String flightId, String bookingId, int seats) {
        flightClient.reserve(flightId, new FlightClient.SeatsRequest(seats));
        return true;
    }

    public boolean reserveFallback(String flightId, String bookingId, int seats, Throwable t) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Flight service unavailable");
    }

    @CircuitBreaker(name = "flight", fallbackMethod = "releaseFallback")
    public boolean releaseFlight(String flightId, String bookingId, int seats) {
        flightClient.release(flightId, new FlightClient.SeatsRequest(seats));
        return true;
    }

    public boolean releaseFallback(String flightId, String bookingId, int seats, Throwable t) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Flight service unavailable");
    }
}
