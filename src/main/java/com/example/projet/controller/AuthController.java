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
    
    private RestTemplate restTemplate = new RestTemplate();
    
    // ==================== GOOGLE SIGN-IN ====================
    
    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request, 
                                         HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🔵 [GOOGLE LOGIN] Tentative avec token");
        
        if (request.getToken() == null || request.getToken().isEmpty()) {
            response.put("success", false);
            response.put("message", "Google token is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // Vérifier le token avec Google
            String googleUrl = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + request.getToken();
            
            Map<String, Object> googleResponse = restTemplate.getForObject(googleUrl, Map.class);
            
            if (googleResponse == null) {
                response.put("success", false);
                response.put("message", "Invalid Google token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Vérifier si le token est valide
            if (googleResponse.containsKey("error_description")) {
                response.put("success", false);
                response.put("message", "Google token error: " + googleResponse.get("error_description"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            String email = (String) googleResponse.get("email");
            String name = (String) googleResponse.get("name");
            String googleId = (String) googleResponse.get("sub");
            
            System.out.println("📧 Google Email: " + email);
            System.out.println("👤 Google Name: " + name);
            System.out.println("🆔 Google ID: " + googleId);
            
            // Vérifier si l'utilisateur existe déjà par email
            Optional<User> userOptional = userService.findByEmail(email);
            User user;
            
            if (userOptional.isPresent()) {
                // Utilisateur existant - connexion
                user = userOptional.get();
                System.out.println("✅ [GOOGLE LOGIN] Utilisateur existant: " + user.getUsername());
            } else {
                // Nouvel utilisateur - inscription automatique
                System.out.println("🆕 [GOOGLE LOGIN] Création d'un nouvel utilisateur");
                
                // Créer un username unique à partir du nom
                String baseUsername = name != null ? 
                    name.toLowerCase().replaceAll("[^a-z0-9]", "_") : 
                    email.split("@")[0];
                
                String username = baseUsername;
                int counter = 1;
                
                // S'assurer que le username est unique
                while (userService.existsByUsername(username)) {
                    username = baseUsername + counter;
                    counter++;
                }
                
                // Créer le nouvel utilisateur
                user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword("GOOGLE_AUTH_" + googleId); // Mot de passe spécial pour identification
                user.setRole("USER");
                user.setActive(true); // Pas besoin de vérification email pour Google
                
                user = userService.save(user);
                System.out.println("✅ [GOOGLE LOGIN] Nouvel utilisateur créé: " + username);
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
            
            System.out.println("✅ [GOOGLE LOGIN] Connexion réussie: " + user.getUsername());
            
            // Préparer la réponse sans mot de passe
            user.setPassword(null);
            
            response.put("success", true);
            response.put("message", "Google login successful");
            response.put("user", user);
            response.put("sessionId", session.getId());
            response.put("isGoogleLogin", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ [GOOGLE LOGIN] Erreur: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Google login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // ==================== VÉRIFICATION EMAIL PAR OTP ====================
    
    @PostMapping("/send-verification-email")
    public ResponseEntity<?> sendVerificationEmail(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String email = request.get("email");
        
        System.out.println("📧 [SEND OTP] Demande pour: " + email);
        
        if (email == null || email.isEmpty()) {
            response.put("success", false);
            response.put("message", "Email is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Vérifier si l'email existe dans la base
        Optional<User> userOptional = userService.findByEmail(email);
        if (!userOptional.isPresent()) {
            response.put("success", false);
            response.put("message", "No user found with this email");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // Générer un OTP
            String otp = otpService.generateOtp(email);
            
            // Envoyer l'OTP par email
            boolean emailSent = emailService.sendOtpEmail(email, otp);
            
            if (emailSent) {
                response.put("success", true);
                response.put("message", "Verification code sent to email");
                response.put("email", email);
                System.out.println("✅ [SEND OTP] Code envoyé à: " + email);
            } else {
                response.put("success", false);
                response.put("message", "Failed to send verification email");
                System.out.println("❌ [SEND OTP] Échec d'envoi à: " + email);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ [SEND OTP] Exception: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Error sending verification email");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        String email = request.getEmail();
        String otp = request.getOtp();
        
        System.out.println("🔐 [VERIFY EMAIL] Tentative pour: " + email);
        
        if (email == null || otp == null) {
            response.put("success", false);
            response.put("message", "Email and OTP are required");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Valider l'OTP
        boolean isValid = otpService.validateOtp(email, otp);
        
        if (isValid) {
            // Trouver l'utilisateur
            Optional<User> userOptional = userService.findByEmail(email);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                
                // Activer l'utilisateur
                user.setActive(true);
                userService.save(user);
                
                response.put("success", true);
                response.put("message", "Email verified successfully");
                response.put("user", user);
                
                System.out.println("✅ [VERIFY EMAIL] Email vérifié: " + email);
            } else {
                response.put("success", false);
                response.put("message", "User not found");
            }
        } else {
            response.put("success", false);
            response.put("message", "Invalid or expired verification code");
            System.out.println("❌ [VERIFY EMAIL] Code invalide pour: " + email);
        }
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== MODIFICATION DE L'INSCRIPTION ====================
    
    // 1. INSCRIPTION PUBLIQUE (avec vérification email)
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
        
        // Désactiver le compte jusqu'à vérification email
        user.setActive(false);
        
        try {
            User savedUser = userService.save(user);
            
            // Générer et envoyer OTP
            String otp = otpService.generateOtp(savedUser.getEmail());
            emailService.sendOtpEmail(savedUser.getEmail(), otp);
            
            response.put("success", true);
            response.put("message", "Account created. Please verify your email.");
            response.put("requiresVerification", true);
            response.put("email", savedUser.getEmail());
            
            // Retourner l'utilisateur sans mot de passe
            savedUser.setPassword(null);
            response.put("user", savedUser);
            
            System.out.println("✅ [PUBLIC REGISTER] Client créé: " + savedUser.getUsername());
            System.out.println("📧 OTP envoyé à: " + savedUser.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // ==================== MODIFICATION DE LA CONNEXION ====================
    
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
            
            // VÉRIFICATION EMAIL - NOUVEAU
            if (!user.isActive()) {
                System.out.println("⚠️  [LOGIN] Email non vérifié pour: " + user.getUsername());
                
                // Générer un nouvel OTP
                String otp = otpService.generateOtp(user.getEmail());
                emailService.sendOtpEmail(user.getEmail(), otp);
                
                response.put("success", false);
                response.put("message", "Please verify your email first");
                response.put("requiresVerification", true);
                response.put("email", user.getEmail());
                return ResponseEntity.ok(response);
            }
            
            // Détecter si c'est une requête du FRONTEND ou BACKEND
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            boolean isFrontendRequest = (origin != null && origin.contains(":3000")) || 
                                        (referer != null && referer.contains(":3000"));
            
            User existingSessionUser = (User) session.getAttribute("currentUser");
            
            if (existingSessionUser != null) {
                System.out.println("   ℹ️  Utilisateur déjà en session: " + existingSessionUser.getUsername());
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
            response.put("isFrontend", isFrontendRequest);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    // ==================== MÉTHODES EXISTANTES (GARDÉES) ====================
    
    // 2. INSCRIPTION NORMALE (gardée pour compatibilité)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🟡 [REGISTER] Inscription normale - Session ID: " + session.getId());
        
        // Vérifier si le username existe déjà
        if (userService.existsByUsername(user.getUsername())) {
            response.put("success", false);
            response.put("message", "Username already exists");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Définir un rôle par défaut si non spécifié
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }
        
        try {
            User savedUser = userService.save(user);
            
            // NE PAS définir la session pour les inscriptions normales
            // Seulement pour login
            
            response.put("success", true);
            response.put("message", "User registered successfully");
            
            savedUser.setPassword(null);
            response.put("user", savedUser);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // NOUVEAU: Endpoint spécifique pour le FRONTEND
    @PostMapping("/frontend/login")
    public ResponseEntity<?> frontendLogin(@RequestBody LoginRequest loginRequest, 
                                           HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🌐 [FRONTEND LOGIN] Tentative: " + loginRequest.getUsername());
        
        Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());
        
        if (userOptional.isPresent() && 
            userOptional.get().getPassword().equals(loginRequest.getPassword())) {
            
            User user = userOptional.get();
            
            // Vérification email
            if (!user.isActive()) {
                String otp = otpService.generateOtp(user.getEmail());
                emailService.sendOtpEmail(user.getEmail(), otp);
                
                response.put("success", false);
                response.put("message", "Please verify your email first");
                response.put("requiresVerification", true);
                response.put("email", user.getEmail());
                return ResponseEntity.ok(response);
            }
            
            // Créer une NOUVELLE session pour le frontend
            HttpSession newSession = request.getSession(true);
            newSession.setMaxInactiveInterval(30 * 60); // 30 minutes
            
            System.out.println("   Nouvelle session frontend: " + newSession.getId());
            
            // Créer un objet user sans mot de passe pour la session
            User sessionUser = new User();
            sessionUser.setId(user.getId());
            sessionUser.setUsername(user.getUsername());
            sessionUser.setEmail(user.getEmail());
            sessionUser.setRole(user.getRole());
            sessionUser.setActive(user.isActive());
            sessionUser.setCreatedAt(user.getCreatedAt());
            
            // Stocker dans la nouvelle session
            newSession.setAttribute("currentUser", sessionUser);
            
            System.out.println("✅ [FRONTEND LOGIN] Connexion réussie: " + user.getUsername());
            
            user.setPassword(null);
            
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("user", user);
            response.put("sessionType", "frontend");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    // ==================== ADMIN SESSION ====================
    
    @GetMapping("/check-admin-session")
    public ResponseEntity<?> checkAdminSession(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null && "ADMIN".equals(currentUser.getRole())) {
            response.put("adminConnected", true);
            response.put("adminUsername", currentUser.getUsername());
            response.put("sessionId", session.getId());
        } else {
            response.put("adminConnected", false);
        }
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== AUTRES MÉTHODES EXISTANTES ====================
    
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
    
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<User> user = userService.findById(id);
        if (user.isPresent()) {
            user.get().setPassword(null);
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.notFound().build();
    }
    
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        
        Map<String, Object> response = new HashMap<>();
        
        Optional<User> userOptional = userService.findById(id);
        if (!userOptional.isPresent()) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.notFound().build();
        }
        
        User existingUser = userOptional.get();
        
        if (userDetails.getUsername() != null && 
            !userDetails.getUsername().equals(existingUser.getUsername()) &&
            userService.existsByUsername(userDetails.getUsername())) {
            response.put("success", false);
            response.put("message", "Username already exists");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (userDetails.getUsername() != null) {
            existingUser.setUsername(userDetails.getUsername());
        }
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            existingUser.setPassword(userDetails.getPassword());
        }
        if (userDetails.getEmail() != null) {
            existingUser.setEmail(userDetails.getEmail());
        }
        if (userDetails.getRole() != null) {
            existingUser.setRole(userDetails.getRole());
        }
        
        User updatedUser = userService.save(existingUser);
        updatedUser.setPassword(null);
        
        response.put("success", true);
        response.put("message", "User updated successfully");
        response.put("user", updatedUser);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access denied");
        }
        
        Map<String, Object> response = new HashMap<>();
        
        if (!userService.findById(id).isPresent()) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.notFound().build();
        }
        
        userService.deleteById(id);
        response.put("success", true);
        response.put("message", "User deleted successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check-auth")
    public ResponseEntity<?> checkAuth() {
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("message", "Authentication system is working");
        return ResponseEntity.ok(response);
    }
    
    // DEBUG: Voir l'état de la session
    @GetMapping("/debug/session")
    public ResponseEntity<?> debugSession(HttpSession session, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("sessionId", session.getId());
        response.put("creationTime", session.getCreationTime());
        response.put("lastAccessedTime", session.getLastAccessedTime());
        response.put("maxInactiveInterval", session.getMaxInactiveInterval());
        response.put("requestOrigin", request.getHeader("Origin"));
        response.put("requestReferer", request.getHeader("Referer"));
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            response.put("currentUser", currentUser);
            response.put("username", currentUser.getUsername());
            response.put("role", currentUser.getRole());
        } else {
            response.put("currentUser", "null");
        }
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== CLASSES DE REQUÊTES ====================
    
    public static class LoginRequest {
        private String username;
        private String password;
        
        public LoginRequest() {}
        
        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class GoogleLoginRequest {
        private String token;
        
        public GoogleLoginRequest() {}
        
        public GoogleLoginRequest(String token) {
            this.token = token;
        }
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
    
    public static class VerifyEmailRequest {
        private String email;
        private String otp;
        
        public VerifyEmailRequest() {}
        
        public VerifyEmailRequest(String email, String otp) {
            this.email = email;
            this.otp = otp;
        }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }
}