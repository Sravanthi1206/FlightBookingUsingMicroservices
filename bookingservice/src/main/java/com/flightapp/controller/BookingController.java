package com.flightapp.controller;

import com.flightapp.model.Booking;
import com.flightapp.service.BookingService;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService svc;

    public BookingController(BookingService svc) { this.svc = svc; }

    @PostMapping("/bookings")
    public ResponseEntity<Booking> create(@RequestBody Booking req) {
        Booking saved = svc.createBooking(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<Booking> get(@PathVariable String id) {
        return ResponseEntity.ok(svc.getBooking(id));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>> getByEmail(@RequestParam(value = "email", required = false) String email) {
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<Booking> list = svc.getBookingsByEmail(email);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<Booking> cancel(@PathVariable String id) {
        return ResponseEntity.ok(svc.cancelBooking(id));
    }
}
