package com.example.projet.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Service
public class OtpService {
    
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 2;
    private static final Random random = new Random();
    
    // Stockage en mémoire des OTPs (email -> OtpData)
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    
    public String generateOtp(String email) {
        // Générer un OTP aléatoire de 6 chiffres
        String otp = String.format("%06d", random.nextInt(999999));
        
        // Calculer la date d'expiration
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        
        // Stocker l'OTP
        otpStorage.put(email, new OtpData(otp, expiryTime));
        
        System.out.println("🔑 OTP généré pour " + email + ": " + otp + 
                          " (expire à: " + expiryTime + ")");
        
        return otp;
    }
    
    public boolean validateOtp(String email, String otp) {
        OtpData otpData = otpStorage.get(email);
        
        if (otpData == null) {
            System.out.println("❌ Pas d'OTP trouvé pour: " + email);
            return false;
        }
        
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            System.out.println("⏰ OTP expiré pour: " + email);
            otpStorage.remove(email);
            return false;
        }
        
        boolean isValid = otpData.otp.equals(otp);
        
        if (isValid) {
            System.out.println("✅ OTP valide pour: " + email);
            // Ne pas supprimer immédiatement pour permettre plusieurs tentatives
        } else {
            System.out.println("❌ OTP invalide pour: " + email);
        }
        
        return isValid;
    }
    
    public void clearOtp(String email) {
        otpStorage.remove(email);
        System.out.println("🧹 OTP supprimé pour: " + email);
    }
    
    public boolean isOtpExpired(String email) {
        OtpData otpData = otpStorage.get(email);
        return otpData == null || LocalDateTime.now().isAfter(otpData.expiryTime);
    }
    
    // Classe interne pour stocker les données OTP
    private static class OtpData {
        String otp;
        LocalDateTime expiryTime;
        
        OtpData(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }
}