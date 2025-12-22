package com.flightapp.service;

import com.flightapp.exception.NotFoundException;
import com.flightapp.model.Flight;
import com.flightapp.repository.FlightRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightService {

    private final FlightRepository repo;

    public FlightService(FlightRepository repo) {
        this.repo = repo;
    }

    public Flight addOrUpdateInventory(String id, Flight payload) {
        payload.setId(id);
        // Basic validations aligned with assignment
        if (payload.getFromPlace() == null || payload.getFromPlace().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "From City cannot be empty");
        if (payload.getToPlace() == null || payload.getToPlace().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "To City cannot be empty");
        if (payload.getFromPlace().equalsIgnoreCase(payload.getToPlace()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "From and To cannot be the same city");
        if (payload.getTotalSeats() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seats Available must be greater than 0");
        if (payload.getAvailableSeats() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Available seats cannot be negative");
        if (payload.getAvailableSeats() == 0)
            payload.setAvailableSeats(payload.getTotalSeats());
        if (payload.getAvailableSeats() > payload.getTotalSeats())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Available seats cannot exceed total seats");
        if (payload.getFlightDate() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date must be selected");
        if (payload.getFlightDate().isBefore(LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date cannot be in the past");
        if (payload.getDepartureTime() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departure Time must be selected");
        if (payload.getArrivalTime() != null && payload.getArrivalTime().isBefore(payload.getDepartureTime()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arrival Time must be after Departure Time");
        if (payload.getPrice() != null && payload.getPrice() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cost must be greater than 0");

        // Duplicate check
        if (repo.existsById(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Flight number already exists");
        return repo.save(payload);
    }

    public List<Flight> search(String from, String to, java.time.LocalDate date) {
        LocalDate today = LocalDate.now();
        List<Flight> results;
        
        if (date == null) {
            results = repo.findByFromPlaceIgnoreCaseAndToPlaceIgnoreCase(from, to);
        } else {
            // Prevent searching for flights on past dates
            if (date.isBefore(today)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot search for flights on past dates");
            }
            results = repo.findByFromPlaceIgnoreCaseAndToPlaceIgnoreCaseAndFlightDate(from, to, date);
        }
        
        // Filter out flights with dates in the past
        return results.stream()
                .filter(flight -> !flight.getFlightDate().isBefore(today))
                .collect(Collectors.toList());
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

        if (f.getDepartureTime() != null && f.getDepartureTime().isBefore(Instant.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reserve seats for past flights");

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
