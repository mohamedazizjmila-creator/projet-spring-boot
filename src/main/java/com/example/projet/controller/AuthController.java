package com.example.projet.controller;

import com.example.projet.entity.User;
import com.example.projet.service.UserService;
import com.example.projet.service.EmailService;
import com.example.projet.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private OtpService otpService;
    
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private final RestTemplate restTemplate = new RestTemplate();
    
    // ==================== INSCRIPTION ====================
    
    // 1. INSCRIPTION PUBLIQUE (pour les clients frontend)
    @PostMapping("/public/register")
    public ResponseEntity<?> publicRegister(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🔵 [PUBLIC REGISTER] Nouvelle inscription client");
        
        // Vérifier si le username existe déjà
        if (userService.existsByUsername(user.getUsername())) {
            response.put("success", false);
            response.put("message", "Username already exists");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Vérifier si l'email existe déjà
        if (userService.existsByEmail(user.getEmail())) {
            response.put("success", false);
            response.put("message", "Email already exists");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Définir un rôle par défaut si non spécifié
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER"); // Rôle client standard
        }
        
        // S'assurer que ce n'est pas un admin
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            user.setRole("USER"); // Forcer le rôle USER pour les inscriptions publiques
        }
        
        try {
            User savedUser = userService.save(user);
            
            // Générer un OTP pour la vérification email
            String otp = otpService.generateOtp(savedUser.getEmail());
            
            // Envoyer l'OTP par email
            boolean emailSent = emailService.sendOtpEmail(savedUser.getEmail(), otp);
            
            if (!emailSent) {
                System.out.println("⚠️  Échec d'envoi de l'email OTP");
            }
            
            response.put("success", true);
            response.put("message", "Account created successfully. Please verify your email.");
            response.put("email", savedUser.getEmail());
            response.put("requiresVerification", true);
            
            // Retourner l'utilisateur sans mot de passe
            savedUser.setPassword(null);
            response.put("user", savedUser);
            
            System.out.println("✅ [PUBLIC REGISTER] Client créé: " + savedUser.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // ==================== VÉRIFICATION EMAIL ====================
    
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody EmailVerificationRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("📧 [EMAIL VERIFICATION] Tentative pour: " + request.getEmail());
        
        if (request.getEmail() == null || request.getOtp() == null) {
            response.put("success", false);
            response.put("message", "Email and OTP are required");
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtp());
        
        if (isValid) {
            // Trouver l'utilisateur par email
            Optional<User> userOptional = userService.findByEmail(request.getEmail());
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                
                // Marquer l'email comme vérifié
                user.setActive(true);
                userService.save(user);
                
                // Supprimer l'OTP après utilisation
                otpService.clearOtp(request.getEmail());
                
                response.put("success", true);
                response.put("message", "Email verified successfully");
                response.put("user", user);
                
                System.out.println("✅ [EMAIL VERIFICATION] Succès pour: " + request.getEmail());
            } else {
                response.put("success", false);
                response.put("message", "User not found");
            }
        } else {
            response.put("success", false);
            response.put("message", "Invalid or expired OTP");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String email = request.get("email");
        
        if (email == null || email.isEmpty()) {
            response.put("success", false);
            response.put("message", "Email is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Générer un nouveau OTP
        String otp = otpService.generateOtp(email);
        
        // Envoyer le nouvel OTP par email
        boolean emailSent = emailService.sendOtpEmail(email, otp);
        
        if (emailSent) {
            response.put("success", true);
            response.put("message", "New OTP sent successfully");
            System.out.println("🔄 [RESEND OTP] Nouvel OTP envoyé à: " + email);
        } else {
            response.put("success", false);
            response.put("message", "Failed to send OTP");
        }
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== GOOGLE SIGN IN ====================
    
    @PostMapping("/google-signin")
    public ResponseEntity<?> googleSignIn(@RequestBody GoogleSignInRequest request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🔵 [GOOGLE SIGNIN] Tentative avec token");
        
        if (request.getIdToken() == null || request.getIdToken().isEmpty()) {
            response.put("success", false);
            response.put("message", "Google token is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // Vérifier le token Google
            String tokenInfoUrl = GOOGLE_TOKEN_INFO_URL + "?id_token=" + request.getIdToken();
            Map<String, Object> tokenInfo = restTemplate.getForObject(tokenInfoUrl, Map.class);
            
            if (tokenInfo == null || tokenInfo.get("email") == null) {
                response.put("success", false);
                response.put("message", "Invalid Google token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            String email = (String) tokenInfo.get("email");
            String name = (String) tokenInfo.get("name");
            String googleId = (String) tokenInfo.get("sub");
            
            System.out.println("📧 Google Email: " + email);
            System.out.println("👤 Google Name: " + name);
            
            // Chercher l'utilisateur par email
            Optional<User> userOptional = userService.findByEmail(email);
            User user;
            
            if (userOptional.isPresent()) {
                // Utilisateur existant - connexion
                user = userOptional.get();
                System.out.println("✅ [GOOGLE SIGNIN] Utilisateur existant trouvé: " + user.getUsername());
            } else {
                // Nouvel utilisateur - inscription
                System.out.println("🆕 [GOOGLE SIGNIN] Création d'un nouvel utilisateur");
                
                // Générer un username unique à partir du nom Google
                String baseUsername = name.toLowerCase().replaceAll("[^a-z0-9]", "");
                String username = baseUsername;
                int counter = 1;
                
                while (userService.existsByUsername(username)) {
                    username = baseUsername + counter;
                    counter++;
                }
                
                // Créer le nouvel utilisateur
                user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword("GOOGLE_OAUTH_" + googleId); // Mot de passe spécial pour Google
                user.setRole("USER");
                user.setActive(true);
                
                user = userService.save(user);
                System.out.println("✅ [GOOGLE SIGNIN] Nouvel utilisateur créé: " + username);
            }
            
            // Créer un objet user sans mot de passe pour la session
            User sessionUser = new User();
            sessionUser.setId(user.getId());
            sessionUser.setUsername(user.getUsername());
            sessionUser.setEmail(user.getEmail());
            sessionUser.setRole(user.getRole());
            sessionUser.setActive(user.isActive());
            sessionUser.setCreatedAt(user.getCreatedAt());
            
            // Stocker dans la session HTTP
            session.setAttribute("currentUser", sessionUser);
            
            System.out.println("✅ [GOOGLE SIGNIN] Connexion réussie: " + user.getUsername());
            
            // Préparer la réponse
            user.setPassword(null);
            
            response.put("success", true);
            response.put("message", "Google sign-in successful");
            response.put("user", user);
            response.put("sessionId", session.getId());
            response.put("isGoogleSignIn", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ [GOOGLE SIGNIN] Erreur: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Google authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // ==================== CONNEXION NORMALE ====================
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, 
                                   HttpSession session,
                                   HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🔵 [LOGIN] Tentative de connexion: " + loginRequest.getUsername());
        
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            response.put("success", false);
            response.put("message", "Username and password are required");
            return ResponseEntity.badRequest().body(response);
        }
        
        Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());
        
        if (userOptional.isPresent() && 
            userOptional.get().getPassword().equals(loginRequest.getPassword())) {
            
            User user = userOptional.get();
            
            // Vérifier si l'email est vérifié
            if (!user.isActive()) {
                // Générer un nouvel OTP pour la vérification
                String otp = otpService.generateOtp(user.getEmail());
                emailService.sendOtpEmail(user.getEmail(), otp);
                
                response.put("success", false);
                response.put("message", "Email not verified. Please check your email for verification code.");
                response.put("requiresVerification", true);
                response.put("email", user.getEmail());
                return ResponseEntity.ok(response);
            }
            
            User existingSessionUser = (User) session.getAttribute("currentUser");
            
            if (existingSessionUser != null) {
                System.out.println("ℹ️  Utilisateur déjà en session: " + existingSessionUser.getUsername());
            }
            
            // Créer un objet user sans mot de passe pour la session
            User sessionUser = new User();
            sessionUser.setId(user.getId());
            sessionUser.setUsername(user.getUsername());
            sessionUser.setEmail(user.getEmail());
            sessionUser.setRole(user.getRole());
            sessionUser.setActive(user.isActive());
            sessionUser.setCreatedAt(user.getCreatedAt());
            
            // Stocker dans la session HTTP
            session.setAttribute("currentUser", sessionUser);
            
            System.out.println("✅ [LOGIN] Connexion réussie: " + user.getUsername());
            
            // Préparer la réponse sans mot de passe
            user.setPassword(null);
            
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("user", user);
            response.put("sessionId", session.getId());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    // ==================== MÉTHODES EXISTANTES (SANS CHANGEMENTS) ====================
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Map<String, Object> response = new HashMap<>();
        
        if (currentUser != null) {
            response.put("authenticated", true);
            response.put("user", currentUser);
            response.put("sessionId", session.getId());
        } else {
            response.put("authenticated", false);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            return ResponseEntity.ok(currentUser);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
    }
    
    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsername(@PathVariable String username) {
        Map<String, Object> response = new HashMap<>();
        boolean exists = userService.existsByUsername(username);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access denied");
        }
        
        List<User> users = userService.findAll();
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }
    
    // ... autres méthodes existantes (sans changements)
    
    // ==================== CLASSES DE REQUÊTES ====================
    
    public static class LoginRequest {
        private String username;
        private String password;
        
        public LoginRequest() {}
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class EmailVerificationRequest {
        private String email;
        private String otp;
        
        public EmailVerificationRequest() {}
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }
    
    public static class GoogleSignInRequest {
        private String idToken;
        
        public GoogleSignInRequest() {}
        
        public String getIdToken() { return idToken; }
        public void setIdToken(String idToken) { this.idToken = idToken; }
    }
}