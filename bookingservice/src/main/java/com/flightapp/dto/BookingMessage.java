package com.flightapp.dto;

import lombok.Data;

@Data
public class BookingMessage {
    private String id;
    private String userEmail;
    private String flightId;
    private int seats;
}
