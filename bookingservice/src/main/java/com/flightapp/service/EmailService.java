package com.flightapp.service;

import com.flightapp.dto.BookingMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBookingMail(BookingMessage msg) {

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(msg.getUserEmail());
        mail.setSubject("Booking Confirmed: " + msg.getId());
        mail.setText(
            "Your booking is confirmed.\n\n" +
            "Booking ID: " + msg.getId() + "\n" +
            "Flight: " + msg.getFlightId() + "\n" +
            "Seats: " + msg.getSeats()
        );

        mailSender.send(mail);

        System.out.println("✔ Email sent to: " + msg.getUserEmail());
    }
}
