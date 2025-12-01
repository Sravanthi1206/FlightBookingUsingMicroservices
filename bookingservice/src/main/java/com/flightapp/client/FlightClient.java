package com.flightapp.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;

@FeignClient(name = "FLIGHTSERVICE", path = "/api/flights")
public interface FlightClient {

    record SeatsRequest(int seats) {}

    @PostMapping("/{id}/reserve")
    ResponseEntity<Void> reserve(@PathVariable("id") String id, @RequestBody SeatsRequest req);

    @PostMapping("/{id}/release")
    ResponseEntity<Void> release(@PathVariable("id") String id, @RequestBody SeatsRequest req);
}
