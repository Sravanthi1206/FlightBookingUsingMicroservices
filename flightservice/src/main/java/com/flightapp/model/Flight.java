package com.flightapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document("flights")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Flight {
  @Id
  private String id;
  private String fromPlace;
  private String toPlace;
  private int totalSeats;
  private int availableSeats;
  private Instant departureTime;
  private LocalDate flightDate;
  private Instant arrivalTime;
  private Double price;
}

