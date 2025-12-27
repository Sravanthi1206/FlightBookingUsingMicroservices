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
import org.springframework.http.ResponseEntity;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.util.UUID;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepo;
    private final FlightClient flightClient;
    private final KafkaTemplate<String, BookingMessage> kafkaTemplate;
    private final String topic;
    private final long cancellationWindowHours;

    public BookingService(BookingRepository bookingRepo,
                          FlightClient flightClient,
                          KafkaTemplate<String, BookingMessage> kafkaTemplate,
                          @Value("${booking.topic:booking.created}") String topic,
                          @Value("${booking.cancellation.hours:24}") long cancellationWindowHours) {
        this.bookingRepo = bookingRepo;
        this.flightClient = flightClient;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.cancellationWindowHours = cancellationWindowHours;
    }

    public Booking getBooking(String id) {
        return bookingRepo.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    public List<Booking> getBookingsByEmail(String email) {
        return bookingRepo.findByUserEmail(email);
    }

    public Booking createBooking(Booking req) {
        String bookingId = UUID.randomUUID().toString();

        // Validate number of seats matches number of passengers
        if (req.getPassengers() == null || req.getPassengers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one passenger is required");
        }
        if (req.getPassengers().size() != req.getSeats()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of passengers must match number of seats");
        }

        // Validate each passenger and collect seat numbers
        java.util.Set<String> seatNumbersSet = new java.util.HashSet<>();
        java.util.List<String> seatNumbersList = new java.util.ArrayList<>();
        
        for (int i = 0; i < req.getPassengers().size(); i++) {
            var passenger = req.getPassengers().get(i);
            if (passenger.getName() == null || passenger.getName().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passenger " + (i + 1) + " name is required");
            }
            if (passenger.getEmail() == null || passenger.getEmail().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passenger " + (i + 1) + " email is required");
            }
            if (passenger.getPhoneNumber() == null || passenger.getPhoneNumber().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passenger " + (i + 1) + " phone number is required");
            }
            if (passenger.getPassport() == null || passenger.getPassport().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passenger " + (i + 1) + " passport is required");
            }
            if (passenger.getAge() <= 0 || passenger.getAge() > 150) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passenger " + (i + 1) + " age is invalid");
            }
            
            // Validate seat number
            if (passenger.getSeatNumber() == null || passenger.getSeatNumber().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passenger " + (i + 1) + " seat number is required");
            }
            
            // Check for duplicate seat numbers
            if (!seatNumbersSet.add(passenger.getSeatNumber())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate seat number: " + passenger.getSeatNumber());
            }
            seatNumbersList.add(passenger.getSeatNumber());
        }

        // Validate flight - do not allow booking past flights
        ResponseEntity<FlightClient.FlightInfo> flightResp = flightClient.get(req.getFlightId());
        if (!flightResp.getStatusCode().is2xxSuccessful() || flightResp.getBody() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Flight not found");
        }
        FlightClient.FlightInfo flightInfo = flightResp.getBody();
        
        // Check if flight date is in the past
        if (flightInfo.flightDate() != null && flightInfo.flightDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book flights on past dates");
        }
        
        // Check if departure time has already passed
        if (flightInfo.departureTime() != null && flightInfo.departureTime().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book flights that have already departed");
        }

        // Normalize and validate user email
        if (req.getUserEmail() == null || req.getUserEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User email is required");
        }
        req.setUserEmail(req.getUserEmail().trim().toLowerCase());

        boolean reserved = reserveFlightSeats(req.getFlightId(), bookingId, seatNumbersList);
        if (!reserved) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to reserve seats");

        req.setId(bookingId);
        req.setStatus("CONFIRMED");
        req.setCreatedAt(Instant.now());
        req.setSeatNumbers(seatNumbersList);
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

        // check cancellation window based on flight departure
        ResponseEntity<FlightClient.FlightInfo> flightResp = flightClient.get(b.getFlightId());
        if (flightResp.getStatusCode().is2xxSuccessful() && flightResp.getBody() != null) {
            FlightClient.FlightInfo flightInfo = flightResp.getBody();
            if (flightInfo.departureTime() != null) {
                Instant cutoff = flightInfo.departureTime().minus(Duration.ofHours(cancellationWindowHours));
                if (Instant.now().isAfter(cutoff)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel booking within " + cancellationWindowHours + " hours of departure");
                }
            }
        }

        java.util.List<String> seatNumbers = b.getSeatNumbers() != null ? b.getSeatNumbers() : java.util.Collections.emptyList();
        boolean released = !seatNumbers.isEmpty() ? releaseFlightSeats(b.getFlightId(), b.getId(), seatNumbers)
            : releaseFlight(b.getFlightId(), b.getId(), b.getSeats());
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

    @CircuitBreaker(name = "flight", fallbackMethod = "reserveSeatsFallback")
    public boolean reserveFlightSeats(String flightId, String bookingId, java.util.List<String> seatNumbers) {
        flightClient.reserveSeats(flightId, new FlightClient.SeatNumbersRequest(seatNumbers));
        return true;
    }

    public boolean reserveSeatsFallback(String flightId, String bookingId, java.util.List<String> seatNumbers, Throwable t) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Flight service unavailable");
    }

    @CircuitBreaker(name = "flight", fallbackMethod = "releaseSeatsFallback")
    public boolean releaseFlightSeats(String flightId, String bookingId, java.util.List<String> seatNumbers) {
        flightClient.releaseSeats(flightId, new FlightClient.SeatNumbersRequest(seatNumbers));
        return true;
    }

    public boolean releaseSeatsFallback(String flightId, String bookingId, java.util.List<String> seatNumbers, Throwable t) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Flight service unavailable");
    }

    // Count-based release for backward compatibility and fallback
    @CircuitBreaker(name = "flight", fallbackMethod = "releaseFallback")
    public boolean releaseFlight(String flightId, String bookingId, int seats) {
        flightClient.release(flightId, new FlightClient.SeatsRequest(seats));
        return true;
    }

    public boolean releaseFallback(String flightId, String bookingId, int seats, Throwable t) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Flight service unavailable");
    }
}