package com.example.projet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public boolean sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Vérification de votre compte");
            message.setText("Bonjour,\n\n"
                    + "Votre code de vérification est: " + otp + "\n\n"
                    + "Ce code expirera dans 2 minutes.\n\n"
                    + "Si vous n'avez pas créé de compte, veuillez ignorer cet email.\n\n"
                    + "Cordialement,\nL'équipe de support");
            
            mailSender.send(message);
            
            System.out.println("📧 Email OTP envoyé à: " + toEmail);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur d'envoi d'email à " + toEmail + ": " + e.getMessage());
            return false;
        }
    }
}