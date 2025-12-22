@echo off
title Flight Booking - Stop Controller

echo ===============================
echo Stopping Flight Booking Services
echo ===============================

REM Kill only service windows by exact title
for %%T in (
  "API-GATEWAY"
  "EMAIL-SERVICE"
  "BOOKING-SERVICE"
  "FLIGHT-SERVICE"
  "AUTH-SERVICE"
  "CONFIG-SERVER"
  "EUREKA-SERVER"
) do (
  taskkill /FI "WINDOWTITLE eq %%~T*" /T /F >nul 2>&1
)

echo ===============================
echo All service windows closed
echo ===============================

REM Keep this terminal open
echo Done.
