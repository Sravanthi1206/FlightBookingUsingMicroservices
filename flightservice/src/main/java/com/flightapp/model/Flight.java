package com.flightapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
}

