package com.flightapp.controller;

import com.flightapp.model.Flight;
import com.flightapp.service.FlightService;
import com.flightapp.config.RequireRole;
import com.flightapp.dto.SeatNumbersRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat; 

@RestController
@RequestMapping("/api")
public class FlightController {

    private final FlightService svc;

    public FlightController(FlightService svc) {
        this.svc = svc;
    }

    @RequireRole("ADMIN")
    @PutMapping("/flights/{id}/inventory")
    public ResponseEntity<Flight> addInventory(
            @PathVariable String id,
            @RequestBody Flight payload) {

        Flight saved = svc.addOrUpdateInventory(id, payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/flights")
    public List<Flight> search(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return svc.search(from, to, date);
    }

    @GetMapping("/flights/{id}")
    public ResponseEntity<Flight> getById(@PathVariable String id) {
        return ResponseEntity.ok(svc.getById(id));
    }

    @GetMapping("/flights/{id}/booked-seats")
    public ResponseEntity<List<String>> getBookedSeats(@PathVariable String id) {
        return ResponseEntity.ok(svc.getBookedSeats(id));
    }

    @PostMapping("/flights/{id}/reserve-seats")
    public ResponseEntity<Void> reserveSeats(
            @PathVariable String id,
            @RequestBody SeatNumbersRequest req) {
        svc.reserveSeats(id, req.getSeatNumbers());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/flights/{id}/release-seats")
    public ResponseEntity<Void> releaseSeats(
            @PathVariable String id,
            @RequestBody SeatNumbersRequest req) {
        svc.releaseSeats(id, req.getSeatNumbers());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/flights/{id}/reserve")
    public ResponseEntity<Void> reserve(
            @PathVariable String id,
            @RequestBody Map<String, Integer> body) {

        svc.reserve(id, body.get("seats"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/flights/{id}/release")
    public ResponseEntity<Void> release(
            @PathVariable String id,
            @RequestBody Map<String, Integer> body) {

        svc.release(id, body.get("seats"));
        return ResponseEntity.ok().build();
    }
}
