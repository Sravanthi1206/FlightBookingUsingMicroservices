package com.flightapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingMessage {

    private String id;
    private String userEmail;
    private String flightId;
    private int seats;
}
