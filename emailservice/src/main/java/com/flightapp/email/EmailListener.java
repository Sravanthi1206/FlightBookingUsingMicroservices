package com.flightapp.email;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.flightapp.dto.BookingMessage;

@Component
public class EmailListener {

    private final EmailService emailService;

    public EmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "booking.created", groupId = "email-group")
    public void consumeBookingCreated(BookingMessage msg) {
        System.out.println("EmailListener received: " + msg);
        emailService.sendBookingEmail(msg);
    }
}
