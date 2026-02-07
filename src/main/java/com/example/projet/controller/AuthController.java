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
    
    // ==================== MÉTHODES SPÉCIFIQUES BACKEND ====================
    
    /**
     * Connexion uniquement pour le BACKEND ADMIN (localhost:8080)
     * Seuls les ADMIN peuvent utiliser cette route
     */
    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest loginRequest, 
                                        HttpSession session,
                                        HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🔐 [ADMIN LOGIN] Tentative de connexion admin: " + loginRequest.getUsername());
        
        // Vérifier que la requête vient bien du backend (pas du frontend React)
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        boolean isFromFrontend = (origin != null && origin.contains(":3000")) || 
                                (referer != null && referer.contains(":3000"));
        
        if (isFromFrontend) {
            System.out.println("🚫 [ADMIN LOGIN] Bloqué - Requête du frontend");
            response.put("success", false);
            response.put("message", "Admin login is only available from backend");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());
        
        if (userOptional.isPresent() && 
            userOptional.get().getPassword().equals(loginRequest.getPassword())) {
            
            User user = userOptional.get();
            
            // VÉRIFICATION CRUCIALE : Seuls les ADMIN peuvent se connecter via cette route
            if (!"ADMIN".equals(user.getRole())) {
                System.out.println("🚫 [ADMIN LOGIN] Bloqué - Rôle USER: " + user.getUsername());
                response.put("success", false);
                response.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Vérifier si l'email est vérifié
            if (!user.isActive()) {
                System.out.println("⚠️  [ADMIN LOGIN] Email non vérifié pour: " + user.getUsername());
                response.put("success", false);
                response.put("message", "Please verify your email first");
                response.put("requiresVerification", true);
                response.put("email", user.getEmail());
                return ResponseEntity.ok(response);
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
            session.setAttribute("isAdminSession", true);
            
            System.out.println("✅ [ADMIN LOGIN] Connexion admin réussie: " + user.getUsername());
            
            // Préparer la réponse sans mot de passe
            user.setPassword(null);
            
            response.put("success", true);
            response.put("message", "Admin login successful");
            response.put("user", user);
            response.put("sessionId", session.getId());
            response.put("sessionType", "admin-backend");
            
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid admin credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    /**
     * Inscription ADMIN (uniquement pour le backend)
     * Ne doit pas être accessible depuis le frontend
     */
    @PostMapping("/admin/register")
    public ResponseEntity<?> adminRegister(@RequestBody User user, 
                                           HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🔐 [ADMIN REGISTER] Création compte admin: " + user.getUsername());
        
        // Vérifier que la requête vient bien du backend
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        boolean isFromFrontend = (origin != null && origin.contains(":3000")) || 
                                (referer != null && referer.contains(":3000"));
        
        if (isFromFrontend) {
            System.out.println("🚫 [ADMIN REGISTER] Bloqué - Requête du frontend");
            response.put("success", false);
            response.put("message", "Admin registration is only available from backend");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
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
        
        // FORCER le rôle ADMIN
        user.setRole("ADMIN");
        user.setActive(true); // Les admin n'ont pas besoin de vérification email
        
        try {
            User savedUser = userService.save(user);
            
            response.put("success", true);
            response.put("message", "Admin account created successfully");
            
            savedUser.setPassword(null);
            response.put("user", savedUser);
            
            System.out.println("✅ [ADMIN REGISTER] Compte admin créé: " + savedUser.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Admin registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // ==================== GOOGLE SIGN-IN ====================
    
    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request, 
                                         HttpSession session,
                                         HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🔵 [GOOGLE LOGIN] Tentative avec token");
        
        // Vérifier si c'est une requête du backend
        String origin = httpRequest.getHeader("Origin");
        String referer = httpRequest.getHeader("Referer");
        boolean isBackendRequest = (origin == null || !origin.contains(":3000")) && 
                                  (referer == null || !referer.contains(":3000"));
        
        if (isBackendRequest) {
            System.out.println("🚫 [GOOGLE LOGIN] Bloqué - Google login non disponible pour le backend");
            response.put("success", false);
            response.put("message", "Google login is only available for frontend users");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
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
            
            // Vérifier si l'utilisateur existe déjà par email
            Optional<User> userOptional = userService.findByEmail(email);
            User user;
            
            if (userOptional.isPresent()) {
                user = userOptional.get();
                System.out.println("✅ [GOOGLE LOGIN] Utilisateur existant: " + user.getUsername());
                
                // Vérifier si c'est un ADMIN qui tente de se connecter via Google
                if ("ADMIN".equals(user.getRole())) {
                    System.out.println("⚠️  [GOOGLE LOGIN] ADMIN détecté - Forcer le logout");
                    session.invalidate();
                    response.put("success", false);
                    response.put("message", "Admin accounts cannot use Google login");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
            } else {
                // NOUVEL UTILISATEUR - TOUJOURS "USER"
                System.out.println("🆕 [GOOGLE LOGIN] Création d'un nouvel utilisateur USER");
                
                String baseUsername = name != null ? 
                    name.toLowerCase().replaceAll("[^a-z0-9]", "_") : 
                    email.split("@")[0];
                
                String username = baseUsername;
                int counter = 1;
                
                while (userService.existsByUsername(username)) {
                    username = baseUsername + counter;
                    counter++;
                }
                
                user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword("GOOGLE_AUTH_" + googleId);
                user.setRole("USER"); // Toujours USER pour Google login
                user.setActive(true);
                
                user = userService.save(user);
                System.out.println("✅ [GOOGLE LOGIN] Nouvel USER créé: " + username);
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
            session.setAttribute("isFrontendSession", true);
            
            System.out.println("✅ [GOOGLE LOGIN] Connexion USER réussie: " + user.getUsername());
            
            user.setPassword(null);
            
            response.put("success", true);
            response.put("message", "Google login successful");
            response.put("user", user);
            response.put("sessionId", session.getId());
            response.put("isGoogleLogin", true);
            response.put("sessionType", "frontend-google");
            
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
    public ResponseEntity<?> sendVerificationEmail(@RequestBody Map<String, String> request,
                                                   HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        
        String email = request.get("email");
        
        System.out.println("📧 [SEND OTP] Demande pour: " + email);
        
        if (email == null || email.isEmpty()) {
            response.put("success", false);
            response.put("message", "Email is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Vérifier si c'est pour un USER (frontend) uniquement
        Optional<User> userOptional = userService.findByEmail(email);
        if (!userOptional.isPresent()) {
            response.put("success", false);
            response.put("message", "No user found with this email");
            return ResponseEntity.badRequest().body(response);
        }
        
        User user = userOptional.get();
        
        // Les ADMIN n'ont pas besoin de vérification email
        if ("ADMIN".equals(user.getRole())) {
            System.out.println("ℹ️  [SEND OTP] ADMIN détecté - Pas de vérification nécessaire");
            response.put("success", true);
            response.put("message", "Admin account does not require email verification");
            response.put("adminAccount", true);
            return ResponseEntity.ok(response);
        }
        
        try {
            String otp = otpService.generateOtp(email);
            boolean emailSent = emailService.sendOtpEmail(email, otp);
            
            if (emailSent) {
                response.put("success", true);
                response.put("message", "Verification code sent to email");
                response.put("email", email);
                System.out.println("✅ [SEND OTP] Code envoyé à USER: " + email);
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
        
        Optional<User> userOptional = userService.findByEmail(email);
        if (!userOptional.isPresent()) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.badRequest().body(response);
        }
        
        User user = userOptional.get();
        
        // Vérifier si c'est un ADMIN
        if ("ADMIN".equals(user.getRole())) {
            System.out.println("ℹ️  [VERIFY EMAIL] ADMIN détecté - Pas de vérification nécessaire");
            response.put("success", true);
            response.put("message", "Admin account does not require email verification");
            response.put("adminAccount", true);
            response.put("user", user);
            return ResponseEntity.ok(response);
        }
        
        // Valider l'OTP pour les USER seulement
        boolean isValid = otpService.validateOtp(email, otp);
        
        if (isValid) {
            user.setActive(true);
            userService.save(user);
            
            response.put("success", true);
            response.put("message", "Email verified successfully");
            response.put("user", user);
            
            System.out.println("✅ [VERIFY EMAIL] Email USER vérifié: " + email);
        } else {
            response.put("success", false);
            response.put("message", "Invalid or expired verification code");
            System.out.println("❌ [VERIFY EMAIL] Code invalide pour: " + email);
        }
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== INSCRIPTION PUBLIC (FRONTEND) ====================
    
    @PostMapping("/public/register")
    public ResponseEntity<?> publicRegister(@RequestBody User user, 
                                            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🔵 [PUBLIC REGISTER] Nouvelle inscription client");
        
        // Vérifier que c'est bien une requête du frontend
        String origin = request.getHeader("Origin");
        boolean isFromFrontend = origin != null && origin.contains(":3000");
        
        if (!isFromFrontend) {
            System.out.println("🚫 [PUBLIC REGISTER] Bloqué - Requête du backend");
            response.put("success", false);
            response.put("message", "Public registration is only available from frontend");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        // Vérifications d'existence
        if (userService.existsByUsername(user.getUsername())) {
            response.put("success", false);
            response.put("message", "Username already exists");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (userService.existsByEmail(user.getEmail())) {
            response.put("success", false);
            response.put("message", "Email already exists");
            return ResponseEntity.badRequest().body(response);
        }
        
        // FORCER le rôle USER pour les inscriptions publiques
        user.setRole("USER");
        
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
            
            savedUser.setPassword(null);
            response.put("user", savedUser);
            
            System.out.println("✅ [PUBLIC REGISTER] USER créé: " + savedUser.getUsername());
            System.out.println("📧 OTP envoyé à: " + savedUser.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // ==================== CONNEXION UNIFIÉE (DÉTECTION AUTO) ====================
    
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
            
            // Détecter la source de la requête
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            boolean isFromFrontend = (origin != null && origin.contains(":3000")) || 
                                    (referer != null && referer.contains(":3000"));
            
            // LOGIQUE DE SÉPARATION DES RÔLES
            if ("ADMIN".equals(user.getRole())) {
                // ADMIN: uniquement depuis le backend
                if (isFromFrontend) {
                    System.out.println("🚫 [LOGIN] ADMIN tentant de se connecter depuis frontend: " + user.getUsername());
                    response.put("success", false);
                    response.put("message", "Admin accounts must login from backend interface");
                    response.put("redirectToBackend", true);
                    return ResponseEntity.ok(response);
                } else {
                    // ADMIN depuis backend - OK
                    System.out.println("✅ [LOGIN] ADMIN connecté depuis backend: " + user.getUsername());
                    
                    // Vérification email (optionnelle pour admin)
                    if (!user.isActive()) {
                        user.setActive(true); // Auto-activer les admin
                        userService.save(user);
                    }
                }
            } else {
                // USER: uniquement depuis le frontend
                if (!isFromFrontend) {
                    System.out.println("🚫 [LOGIN] USER tentant de se connecter depuis backend: " + user.getUsername());
                    response.put("success", false);
                    response.put("message", "User accounts must login from frontend application");
                    response.put("redirectToFrontend", true);
                    return ResponseEntity.ok(response);
                } else {
                    // USER depuis frontend - Vérifier email
                    if (!user.isActive()) {
                        System.out.println("⚠️  [LOGIN] Email non vérifié pour USER: " + user.getUsername());
                        String otp = otpService.generateOtp(user.getEmail());
                        emailService.sendOtpEmail(user.getEmail(), otp);
                        
                        response.put("success", false);
                        response.put("message", "Please verify your email first");
                        response.put("requiresVerification", true);
                        response.put("email", user.getEmail());
                        return ResponseEntity.ok(response);
                    }
                }
            }
            
            // Créer l'objet session
            User sessionUser = new User();
            sessionUser.setId(user.getId());
            sessionUser.setUsername(user.getUsername());
            sessionUser.setEmail(user.getEmail());
            sessionUser.setRole(user.getRole());
            sessionUser.setActive(user.isActive());
            sessionUser.setCreatedAt(user.getCreatedAt());
            
            // Stocker dans la session avec métadonnées
            session.setAttribute("currentUser", sessionUser);
            
            if ("ADMIN".equals(user.getRole())) {
                session.setAttribute("isAdminSession", true);
                session.setAttribute("sessionType", "admin-backend");
            } else {
                session.setAttribute("isFrontendSession", true);
                session.setAttribute("sessionType", "user-frontend");
            }
            
            System.out.println("✅ [LOGIN] Connexion réussie: " + user.getUsername() + 
                              " (Role: " + user.getRole() + ", Source: " + 
                              (isFromFrontend ? "Frontend" : "Backend") + ")");
            
            user.setPassword(null);
            
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("user", user);
            response.put("sessionId", session.getId());
            response.put("sessionType", session.getAttribute("sessionType"));
            response.put("role", user.getRole());
            
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    // ==================== VÉRIFICATION DE SESSION ====================
    
    /**
     * Vérifie si une session admin est active
     */
    @GetMapping("/check-admin-session")
    public ResponseEntity<?> checkAdminSession(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User currentUser = (User) session.getAttribute("currentUser");
        Boolean isAdminSession = (Boolean) session.getAttribute("isAdminSession");
        
        if (currentUser != null && "ADMIN".equals(currentUser.getRole()) && 
            Boolean.TRUE.equals(isAdminSession)) {
            response.put("adminConnected", true);
            response.put("adminUsername", currentUser.getUsername());
            response.put("sessionId", session.getId());
            response.put("sessionType", "admin-backend");
        } else {
            response.put("adminConnected", false);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Vérifie si une session frontend est active
     */
    @GetMapping("/check-frontend-session")
    public ResponseEntity<?> checkFrontendSession(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User currentUser = (User) session.getAttribute("currentUser");
        Boolean isFrontendSession = (Boolean) session.getAttribute("isFrontendSession");
        
        if (currentUser != null && "USER".equals(currentUser.getRole()) && 
            Boolean.TRUE.equals(isFrontendSession)) {
            response.put("frontendConnected", true);
            response.put("username", currentUser.getUsername());
            response.put("sessionId", session.getId());
            response.put("sessionType", "user-frontend");
        } else {
            response.put("frontendConnected", false);
        }
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== PROTECTION DES ENDPOINTS ADMIN ====================
    
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session, HttpServletRequest request) {
        User currentUser = (User) session.getAttribute("currentUser");
        Boolean isAdminSession = (Boolean) session.getAttribute("isAdminSession");
        
        // Vérifier triple sécurité
        if (currentUser == null || 
            !"ADMIN".equals(currentUser.getRole()) || 
            !Boolean.TRUE.equals(isAdminSession)) {
            System.out.println("🚫 [GET USERS] Accès refusé - Session non admin");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access denied. Admin privileges required.");
        }
        
        List<User> users = userService.findAll();
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Boolean isAdminSession = (Boolean) session.getAttribute("isAdminSession");
        
        if (currentUser == null || 
            !"ADMIN".equals(currentUser.getRole()) || 
            !Boolean.TRUE.equals(isAdminSession)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access denied. Admin privileges required.");
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
    
    // ==================== MÉTHODES EXISTANTES ====================
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        String sessionType = (String) session.getAttribute("sessionType");
        System.out.println("🔓 [LOGOUT] Déconnexion de session: " + sessionType);
        
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
            response.put("sessionType", session.getAttribute("sessionType"));
            response.put("role", currentUser.getRole());
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
    
    // ... (autres méthodes existantes restent inchangées)
    
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