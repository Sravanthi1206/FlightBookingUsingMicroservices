package com.flightapp.repository;

import com.flightapp.model.Flight;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.time.LocalDate;

public interface FlightRepository extends MongoRepository<Flight, String> {

    List<Flight> findByFromPlaceIgnoreCaseAndToPlaceIgnoreCase(String from, String to);

    List<Flight> findByFromPlaceIgnoreCaseAndToPlaceIgnoreCaseAndFlightDate(String from, String to, LocalDate flightDate);
} 
