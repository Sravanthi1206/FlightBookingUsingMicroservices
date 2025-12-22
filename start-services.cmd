@echo off
title Flight Booking - Start Services (LOCAL)

echo ===============================
echo Starting Flight Booking System
echo ===============================

REM -------- EUREKA SERVER --------
start "EUREKA-SERVER" cmd /k ^
"java -jar eureka-server\target\eureka-server-1.0.0.jar"

timeout /t 15 /nobreak

REM -------- CONFIG SERVER --------
start "CONFIG-SERVER" cmd /k ^
"java -jar configserver\target\configserver-1.0.0.jar --spring.profiles.active=local"

timeout /t 15 /nobreak

REM -------- AUTH SERVICE --------
start "AUTH-SERVICE" cmd /k ^
"java -jar authservice\target\authservice-1.0.0.jar --spring.profiles.active=local"

timeout /t 10 /nobreak

REM -------- FLIGHT SERVICE --------
start "FLIGHT-SERVICE" cmd /k ^
"java -jar flightservice\target\flightservice-1.0.0.jar --spring.profiles.active=local"

timeout /t 10 /nobreak

REM -------- BOOKING SERVICE --------
start "BOOKING-SERVICE" cmd /k ^
"java -jar bookingservice\target\bookingservice-1.0.0.jar --spring.profiles.active=local"

timeout /t 10 /nobreak

REM -------- EMAIL SERVICE --------
start "EMAIL-SERVICE" cmd /k ^
"java -jar emailservice\target\emailservice-1.0.0.jar --spring.profiles.active=local"

timeout /t 10 /nobreak

REM -------- API GATEWAY --------
start "API-GATEWAY" cmd /k ^
"java -jar apigateway\target\apigateway-1.0.0.jar --spring.profiles.active=local"

echo ===============================
echo All services started
echo ===============================
