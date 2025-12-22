package com.flightapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Passenger {
    private String name;
    private String email;
    private String phoneNumber;
    private String passport;
    private int age;
}
