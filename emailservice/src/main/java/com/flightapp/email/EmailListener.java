package com.flightapp.email;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class EmailListener {

    private final EmailService emailService;

    @Autowired
    public EmailListener(EmailService emailService){
        this.emailService = emailService;
    }

    @KafkaListener(topics = "booking.created", groupId = "email-group")
    public void consumeBookingCreated(BookingMessage msg){
        emailService.sendBookingEmail(msg);
    }
}
