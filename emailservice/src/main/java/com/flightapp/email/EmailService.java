package com.flightapp.email;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import com.flightapp.dto.BookingMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendBookingEmail(BookingMessage msg) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(msg.getUserEmail());
        mail.setSubject("Booking Confirmed: " + msg.getId());
        mail.setText(
            "Booking ID: " + msg.getId() +
            "\nFlight: " + msg.getFlightId() +
            "\nSeats: " + msg.getSeats()
        );
        mailSender.send(mail);
    }
}
