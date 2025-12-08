package com.flightapp.dto;

import java.io.Serializable;

import lombok.Data;

@Data
public class BookingMessage implements Serializable {
    private String id;
    private String userEmail;
    private String flightId;
    private int seats;
}
