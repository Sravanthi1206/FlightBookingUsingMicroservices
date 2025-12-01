package com.flightapp.kafka;

import com.flightapp.dto.BookingMessage;
import com.flightapp.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookingEventListener {

    private final EmailService emailService;

    public BookingEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "booking.created", groupId = "booking-email-group")
    public void consume(BookingMessage msg) {
        System.out.println("Booking event received for: " + msg.getId());
        emailService.sendBookingMail(msg);
    }
}
