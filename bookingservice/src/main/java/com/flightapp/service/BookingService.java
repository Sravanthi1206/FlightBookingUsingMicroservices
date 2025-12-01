package com.flightapp.service;

import com.flightapp.model.Booking;
import com.flightapp.repository.BookingRepository;
import com.flightapp.exception.NotFoundException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepo;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate; 
    private final String topic;

    public BookingService(BookingRepository bookingRepo,
                          RestTemplate restTemplate,
                          KafkaTemplate<String, Object> kafkaTemplate, 
                          @Value("${booking.topic:booking.created}") String topic) {
        this.bookingRepo = bookingRepo;
        this.restTemplate = restTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public Booking getBooking(String id) {
        return bookingRepo.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    public Booking createBooking(Booking req) {
        String bookingId = UUID.randomUUID().toString();
        // 1) reserve seats in Flight service
        boolean reserved = reserveFlight(req.getFlightId(), bookingId, req.getSeats());
        if (!reserved) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to reserve seats");

        // 2) persist booking
        req.setId(bookingId);
        req.setStatus("CONFIRMED");
        req.setCreatedAt(Instant.now());
        Booking saved = bookingRepo.save(req);

        // 3) publish event
        kafkaTemplate.send(topic, saved.getId(), saved);

        return saved;
    }

    public Booking cancelBooking(String id) {
        Booking b = bookingRepo.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        if ("CANCELLED".equalsIgnoreCase(b.getStatus())) return b;

        // release seats
        boolean released = releaseFlight(b.getFlightId(), b.getId(), b.getSeats());
        if (!released) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to contact flight service");

        b.setStatus("CANCELLED");
        Booking updated = bookingRepo.save(b);

        kafkaTemplate.send("booking.cancelled", updated.getId(), updated);
        return updated;
    }

    // Resilience4j circuit breaker + RestTemplate call
    @CircuitBreaker(name = "flight", fallbackMethod = "reserveFallback")
    public boolean reserveFlight(String flightId, String bookingId, int seats) {
        String url = "http://flight-service/api/flights/" + flightId + "/reserve";
        Map<String,Integer> body = Map.of("seats", seats);
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String,Integer>> e = new HttpEntity<>(body, h);
        ResponseEntity<Void> resp = restTemplate.postForEntity(url, e, Void.class);
        return resp.getStatusCode().is2xxSuccessful();
    }

    public boolean reserveFallback(String flightId, String bookingId, int seats, Throwable t) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Flight service unavailable");
    }

    @CircuitBreaker(name = "flight", fallbackMethod = "releaseFallback")
    public boolean releaseFlight(String flightId, String bookingId, int seats) {
        String url = "http://flight-service/api/flights/" + flightId + "/release";
        Map<String,Integer> body = Map.of("seats", seats);
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String,Integer>> e = new HttpEntity<>(body, h);
        ResponseEntity<Void> resp = restTemplate.postForEntity(url, e, Void.class);
        return resp.getStatusCode().is2xxSuccessful();
    }

    public boolean releaseFallback(String flightId, String bookingId, int seats, Throwable t) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Flight service unavailable");
    }
}
