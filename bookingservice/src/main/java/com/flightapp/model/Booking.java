package com.flightapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("bookings")
public class Booking {
    @Id
    private String id;
    private String flightId;
    @Indexed
    private String userEmail;
    private int seats;
    private String status;
    private Instant createdAt;
    private List<Passenger> passengers;
    private List<String> seatNumbers;
}