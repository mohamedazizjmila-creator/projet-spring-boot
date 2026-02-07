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
    
    // ==================== INSCRIPTION FRONTEND (USER SEULEMENT) ====================
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("📝 [REGISTER] Nouvelle inscription depuis frontend: " + user.getUsername());
        
        // Vérifier l'origine de la requête (facultatif mais recommandé)
        String origin = request.getHeader("Origin");
        boolean isFromFrontend = origin != null && origin.contains(":3000");
        
        if (!isFromFrontend) {
            System.out.println("⚠️  [REGISTER] Tentative d'inscription depuis backend: " + user.getUsername());
            // On peut choisir d'accepter quand même ou de refuser
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
        
        // FORCER le rôle USER pour les inscriptions depuis frontend
        user.setRole("USER");
        
        // Activer le compte directement
        user.setActive(true);
        
        try {
            User savedUser = userService.save(user);
            
            // Ne pas renvoyer le mot de passe
            savedUser.setPassword(null);
            
            response.put("success", true);
            response.put("message", "Registration successful");
            response.put("user", savedUser);
            
            System.out.println("✅ [REGISTER] USER créé: " + savedUser.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // ==================== CONNEXION FRONTEND (USER SEULEMENT) ====================
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, 
                                   HttpSession session,
                                   HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🔐 [LOGIN] Tentative depuis frontend: " + loginRequest.getUsername());
        
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            response.put("success", false);
            response.put("message", "Username and password are required");
            return ResponseEntity.badRequest().body(response);
        }
        
        Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());
        
        if (userOptional.isPresent() && 
            userOptional.get().getPassword().equals(loginRequest.getPassword())) {
            
            User user = userOptional.get();
            
            // ✅ IMPORTANT: VÉRIFIER QUE C'EST BIEN UN USER (pas un ADMIN)
            if (!"USER".equals(user.getRole())) {
                System.out.println("🚫 [LOGIN] Rejeté - Rôle ADMIN détecté: " + user.getUsername());
                response.put("success", false);
                response.put("message", "Admin accounts cannot login from frontend");
                response.put("redirectToBackend", true);
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
            session.setAttribute("loginSource", "frontend");
            
            System.out.println("✅ [LOGIN] USER connecté: " + user.getUsername());
            
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
    
    // ==================== CONNEXION ADMIN (BACKEND SEULEMENT) ====================
    
    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest loginRequest, 
                                        HttpSession session,
                                        HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("👑 [ADMIN LOGIN] Tentative depuis backend: " + loginRequest.getUsername());
        
        // Vérifier que ça vient du backend (pas du frontend)
        String origin = request.getHeader("Origin");
        boolean isFromFrontend = origin != null && origin.contains(":3000");
        
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
            
            // ✅ IMPORTANT: VÉRIFIER QUE C'EST BIEN UN ADMIN
            if (!"ADMIN".equals(user.getRole())) {
                System.out.println("🚫 [ADMIN LOGIN] Rejeté - Rôle USER détecté: " + user.getUsername());
                response.put("success", false);
                response.put("message", "User accounts cannot login from admin panel");
                response.put("redirectToFrontend", true);
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
            session.setAttribute("loginSource", "backend");
            session.setAttribute("isAdminSession", true);
            
            System.out.println("✅ [ADMIN LOGIN] ADMIN connecté: " + user.getUsername());
            
            // Préparer la réponse sans mot de passe
            user.setPassword(null);
            
            response.put("success", true);
            response.put("message", "Admin login successful");
            response.put("user", user);
            response.put("sessionId", session.getId());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid admin credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    // ==================== VÉRIFICATION DE SESSION (avec restriction) ====================
    
    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession(HttpSession session, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        User currentUser = (User) session.getAttribute("currentUser");
        String loginSource = (String) session.getAttribute("loginSource");
        
        if (currentUser != null) {
            // Vérifier la cohérence de la session
            String origin = request.getHeader("Origin");
            boolean isFromFrontend = origin != null && origin.contains(":3000");
            
            if (isFromFrontend && "backend".equals(loginSource)) {
                // Session admin détectée depuis frontend → invalider
                System.out.println("🚫 [CHECK SESSION] Session admin détectée depuis frontend - Déconnexion");
                session.invalidate();
                response.put("authenticated", false);
                response.put("message", "Admin session detected from frontend - Logged out");
            } else if (!isFromFrontend && "frontend".equals(loginSource)) {
                // Session user détectée depuis backend → invalider
                System.out.println("🚫 [CHECK SESSION] Session user détectée depuis backend - Déconnexion");
                session.invalidate();
                response.put("authenticated", false);
                response.put("message", "User session detected from backend - Logged out");
            } else {
                // Session cohérente
                response.put("authenticated", true);
                response.put("user", currentUser);
                response.put("sessionId", session.getId());
                response.put("loginSource", loginSource);
            }
        } else {
            response.put("authenticated", false);
        }
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== UTILITAIRES ====================
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            System.out.println("👋 [LOGOUT] Déconnexion: " + currentUser.getUsername());
        }
        session.invalidate();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(HttpSession session, HttpServletRequest request) {
        User currentUser = (User) session.getAttribute("currentUser");
        String loginSource = (String) session.getAttribute("loginSource");
        
        if (currentUser != null) {
            // Vérifier la cohérence
            String origin = request.getHeader("Origin");
            boolean isFromFrontend = origin != null && origin.contains(":3000");
            
            if ((isFromFrontend && "backend".equals(loginSource)) || 
                (!isFromFrontend && "frontend".equals(loginSource))) {
                session.invalidate();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session mismatch");
            }
            
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
    
    // ==================== ADMIN ENDPOINTS (protégés) ====================
    
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Boolean isAdminSession = (Boolean) session.getAttribute("isAdminSession");
        
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole()) || 
            !Boolean.TRUE.equals(isAdminSession)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access denied");
        }
        
        List<User> users = userService.findAll();
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
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
}