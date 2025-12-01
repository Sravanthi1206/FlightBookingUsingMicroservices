package com.flightapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("bookings")
public class Booking {
    @Id
    private String id;
    private String flightId;
    private String userEmail;
    private int seats;
    private String status;
    private Instant createdAt;
}
