# Flight Booking Microservices System

A microservices-based Flight Booking application built using Spring Boot, Spring Cloud, Eureka, API Gateway, Config Server, MongoDB, and Kafka.

---

## 1. Overview

This project demonstrates a complete microservices architecture where each service is independently deployable, scalable, and discoverable through Eureka.  
The system supports operations like searching flights, booking flights, canceling bookings, and sending email notifications using Kafka.

---

## 2. Services Included

| Service | Port | Description |
|--------|------|-------------|
| Eureka Server | 8761 | Service registry |
| Config Server | 8888 | Centralized configuration |
| API Gateway | 8080 | Single entry point for all services |
| Flight Service | 8100 | Manages flights and inventory |
| Booking Service | 8200 | Manages flight bookings & publishes Kafka events |
| Email Service | 8300 | Consumes booking events & sends notifications |
| Kafka Broker | 9092 | Handles event communication |

---

## 3. Requirements

Before running the system, ensure the following are installed:

### **Software Requirements**
- Java 17
- Apache Maven 3.8+
- MongoDB Community Server
- Apache Kafka & Zookeeper
- Git
- IDE (Eclipse / IntelliJ / VS Code)



## 4. Project Structure

```
FlightBookingMicroserviceWorkspace/
│
├── Eureka-server/
├── config-server/
│     └── config-repo/
│            ├── flight-service.yml
│            ├── booking-service.yml
│            └── apigateway.yml
├── api-gateway/
├── Flight-service/
├── Booking-service/
└── Email-service/
```

---

## 5. How to Run the Project

### Step 1 — Start MongoDB
```
mongod
```

### Step 2 — Start Zookeeper
```
zookeeper-server-start.sh config/zookeeper.properties
```

### Step 3 — Start Kafka
```
kafka-server-start.sh config/server.properties
```

### Step 4 — Start Services in the following order

1. Eureka Server  
   ```
   mvn spring-boot:run
   ```
2. Config Server  
3. API Gateway  
4. Flight Service  
5. Booking Service  
6. Email Service  

---

## 6. Important Endpoints

### **API Gateway Prefix**
All requests go through:
```
http://localhost:8080
```

### **Flight Service**
```
POST /api/flights
GET /api/flights
GET /api/flights/{id}
POST /api/flights/{id}/reserve
POST /api/flights/{id}/release
```

### **Booking Service**
```
POST /api/bookings
GET /api/bookings/{id}
DELETE /api/bookings/{id}
```

---

## 7. Kafka Event Flow

1. Booking created in Booking Service  
2. Booking event published to Kafka topic `booking.created`  
3. Email service consumes the event  
4. Email notification sent  

---

## 8. Notes

- Configurations are fetched dynamically from Config Server.
- All services register themselves in Eureka.
- API Gateway uses service discovery for routing.
- Resilience4j is used for circuit breakers on inter-service communication.

---

