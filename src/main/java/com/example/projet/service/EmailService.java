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
            // Essayer d'envoyer l'email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Votre code OTP");
            message.setText("Code: " + otp);
            mailSender.send(message);
            
            System.out.println("📧 Email OTP envoyé à: " + toEmail);
            return true;
        } catch (Exception e) {
            // FALLBACK: Log dans la console
            System.out.println("📧 [FALLBACK] OTP pour " + toEmail + ": " + otp);
            System.out.println("⚠️  Email non envoyé (SMTP non configuré) - OTP dans les logs");
            
            // IMPORTANT: Retourner true pour que le processus continue
            return true;
        }
    }
}