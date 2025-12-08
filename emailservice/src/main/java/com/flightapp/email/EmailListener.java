package com.flightapp.email;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.flightapp.dto.BookingMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class EmailListener {

    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public EmailListener(EmailService emailService){
        this.emailService = emailService;
    }

    @KafkaListener(topics = "booking.created")
    public void consumeBookingCreated(String msgJson){
        try {
            BookingMessage msg = objectMapper.readValue(msgJson, BookingMessage.class);
            System.out.println("EmailListener received: " + msg);
            emailService.sendBookingEmail(msg);
        } catch (Exception e) {
            // log error and rethrow if you want the DefaultErrorHandler to handle it
            System.err.println("Failed to parse booking message: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
