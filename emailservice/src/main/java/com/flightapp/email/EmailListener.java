package com.flightapp.email;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flightapp.dto.BookingMessage;

@Component
public class EmailListener {

    private static final Logger logger = LoggerFactory.getLogger(EmailListener.class);

    private final EmailService emailService;

    public EmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "booking.created", groupId = "email-group")
    public void consumeBookingCreated(BookingMessage msg) {
        logger.info("EmailListener received booking message: {}", msg);
        emailService.sendBookingEmail(msg);
    }
}