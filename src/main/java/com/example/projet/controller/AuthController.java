package com.example.projet.controller;

import com.example.projet.entity.User;
import com.example.projet.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
            
            // IMPORTANT: Ne pas toucher à la session
            // Juste créer l'utilisateur dans la base de données
            
            response.put("success", true);
            response.put("message", "Account created successfully");
            
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
    
    // ==================== CONNEXION ====================
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, 
                                   HttpSession session,
                                   HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🔵 [LOGIN] Tentative de connexion: " + loginRequest.getUsername());
        System.out.println("   Session ID: " + session.getId());
        
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            response.put("success", false);
            response.put("message", "Username and password are required");
            return ResponseEntity.badRequest().body(response);
        }
        
        Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());
        
        if (userOptional.isPresent() && 
            userOptional.get().getPassword().equals(loginRequest.getPassword())) {
            
            User user = userOptional.get();
            
            // Détecter si c'est une requête du FRONTEND ou BACKEND
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            boolean isFrontendRequest = (origin != null && origin.contains(":3000")) || 
                                        (referer != null && referer.contains(":3000"));
            
            System.out.println("   Origin: " + origin);
            System.out.println("   Referer: " + referer);
            System.out.println("   Is Frontend: " + isFrontendRequest);
            
            User existingSessionUser = (User) session.getAttribute("currentUser");
            
            // CORRECTION PRINCIPALE : Permettre toujours la connexion frontend
            if (existingSessionUser != null) {
                System.out.println("   ℹ️  Utilisateur déjà en session: " + existingSessionUser.getUsername());
                
                // Si c'est le FRONTEND qui veut se connecter
                if (isFrontendRequest) {
                    System.out.println("   🔄 Connexion frontend détectée - Autorisation automatique");
                    
                    // CAS 1: Même utilisateur frontend
                    if (existingSessionUser.getUsername().equals(user.getUsername())) {
                        System.out.println("   ✅ Même utilisateur frontend");
                    }
                    // CAS 2: Différent utilisateur frontend
                    else {
                        System.out.println("   🔄 Nouvel utilisateur frontend - Remplacement de session");
                        // On écrase la session avec le nouvel utilisateur frontend
                    }
                }
                // Si c'est le BACKEND qui veut se connecter
                else {
                    System.out.println("   🔄 Connexion backend détectée");
                    
                    // Si un ADMIN backend est déjà connecté et un autre ADMIN veut se connecter
                    if ("ADMIN".equals(existingSessionUser.getRole()) && "ADMIN".equals(user.getRole())) {
                        System.out.println("   ⚠️  Admin backend déjà connecté - Remplacement");
                    }
                    // Si un USER backend est connecté et un ADMIN veut se connecter
                    else if ("USER".equals(existingSessionUser.getRole()) && "ADMIN".equals(user.getRole())) {
                        System.out.println("   🔄 Admin remplace user backend");
                    }
                }
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
            
            System.out.println("✅ [LOGIN] Connexion réussie: " + user.getUsername() + 
                              " (Role: " + user.getRole() + ")");
            System.out.println("   Session ID: " + session.getId());
            
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
    
    // Vérifier si un admin est connecté
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
    
    // ==================== MÉTHODES EXISTANTES ====================
    
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
    
    // ==================== CLASSE INTERNE ====================
    
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
}