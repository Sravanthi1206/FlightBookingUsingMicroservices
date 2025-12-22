package com.flightapp.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;

@FeignClient(name = "flightservice", path = "/api/flights")
public interface FlightClient {

    record SeatsRequest(int seats) {}

    record FlightInfo(String id, String fromPlace, String toPlace, int totalSeats, int availableSeats, java.time.LocalDate flightDate, java.time.Instant departureTime) {}

    @GetMapping("/{id}")
    ResponseEntity<FlightInfo> get(@PathVariable("id") String id);

    @PostMapping("/{id}/reserve")
    ResponseEntity<Void> reserve(@PathVariable("id") String id, @RequestBody SeatsRequest req);

    @PostMapping("/{id}/release")
    ResponseEntity<Void> release(@PathVariable("id") String id, @RequestBody SeatsRequest req);
}
