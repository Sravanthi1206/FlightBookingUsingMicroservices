package com.flightapp.email;

import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class EmailService {

  private final JavaMailSender mailSender;

  @Autowired
  public EmailService(JavaMailSender mailSender){
    this.mailSender = mailSender;
  }

  public void sendBookingEmail(BookingMessage msg){
    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setTo(msg.getUserEmail());
    mail.setSubject("Booking confirmed: " + msg.getId());
    mail.setText("Booking ID: " + msg.getId()
      + "\nFlight: " + msg.getFlightId()
      + "\nSeats: " + msg.getSeats());
    mailSender.send(mail);
  }
}
