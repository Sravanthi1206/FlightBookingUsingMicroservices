package com.flightapp.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import java.util.List;

@FeignClient(name = "flightservice", path = "/api/flights")
public interface FlightClient {

    record SeatsRequest(int seats) {}

    class SeatNumbersRequest {
        private List<String> seatNumbers;
        
        public SeatNumbersRequest() {}
        
        public SeatNumbersRequest(List<String> seatNumbers) {
            this.seatNumbers = seatNumbers;
        }
        
        public List<String> getSeatNumbers() {
            return seatNumbers;
        }
        
        public void setSeatNumbers(List<String> seatNumbers) {
            this.seatNumbers = seatNumbers;
        }
    }

    record FlightInfo(String id, String fromPlace, String toPlace, int totalSeats, int availableSeats, java.time.LocalDate flightDate, java.time.Instant departureTime) {}

    @GetMapping("/{id}")
    ResponseEntity<FlightInfo> get(@PathVariable("id") String id);

    @PostMapping("/{id}/reserve")
    ResponseEntity<Void> reserve(@PathVariable("id") String id, @RequestBody SeatsRequest req);

    @PostMapping("/{id}/release")
    ResponseEntity<Void> release(@PathVariable("id") String id, @RequestBody SeatsRequest req);

    @PostMapping("/{id}/reserve-seats")
    ResponseEntity<Void> reserveSeats(@PathVariable("id") String id, @RequestBody SeatNumbersRequest req);

    @PostMapping("/{id}/release-seats")
    ResponseEntity<Void> releaseSeats(@PathVariable("id") String id, @RequestBody SeatNumbersRequest req);
}
