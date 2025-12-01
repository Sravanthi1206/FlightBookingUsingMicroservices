package com.flightapp.service;

import com.flightapp.exception.NotFoundException;
import com.flightapp.model.Flight;
import com.flightapp.repository.FlightRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.List;

@Service
public class FlightService {

    private final FlightRepository repo;

    public FlightService(FlightRepository repo) {
        this.repo = repo;
    }

    public Flight addOrUpdateInventory(String id, Flight payload) {
        payload.setId(id);
        if (payload.getAvailableSeats() == 0)
            payload.setAvailableSeats(payload.getTotalSeats());
        return repo.save(payload);
    }

    public List<Flight> search(String from, String to) {
        return repo.findByFromPlaceIgnoreCaseAndToPlaceIgnoreCase(from, to);
    }

    public Flight getById(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Flight not found"));
    }

    @CircuitBreaker(name = "flight", fallbackMethod = "reserveFallback")
    public void reserve(String flightId, int seats) {
        Flight f = getById(flightId);

        if (seats <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seats must be > 0");

        if (f.getAvailableSeats() < seats)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough seats");

        f.setAvailableSeats(f.getAvailableSeats() - seats);
        repo.save(f);
    }

    // fallback signature must match original args + Throwable
    public void reserveFallback(String flightId, int seats, Throwable t) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Flight service temporarily unavailable");
    }

    @CircuitBreaker(name = "flight", fallbackMethod = "releaseFallback")
    public void release(String flightId, int seats) {
        Flight f = getById(flightId);

        if (seats <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seats must be > 0");

        f.setAvailableSeats(Math.min(
                f.getTotalSeats(),
                f.getAvailableSeats() + seats
        ));

        repo.save(f);
    }

    public void releaseFallback(String flightId, int seats, Throwable t) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Flight service temporarily unavailable");
    }
}
