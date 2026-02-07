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
    
    // ==================== INSCRIPTION SIMPLE ====================
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("📝 [REGISTER] Nouvelle inscription: " + user.getUsername());
        
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
        
        // Définir le rôle par défaut (USER)
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }
        
        // Activer le compte directement (pas de vérification email)
        user.setActive(true);
        
        try {
            User savedUser = userService.save(user);
            
            // Ne pas renvoyer le mot de passe
            savedUser.setPassword(null);
            
            response.put("success", true);
            response.put("message", "Registration successful");
            response.put("user", savedUser);
            
            System.out.println("✅ [REGISTER] Utilisateur créé: " + savedUser.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // ==================== CONNEXION SIMPLE ====================
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, 
                                   HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("🔐 [LOGIN] Tentative: " + loginRequest.getUsername());
        
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            response.put("success", false);
            response.put("message", "Username and password are required");
            return ResponseEntity.badRequest().body(response);
        }
        
        Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());
        
        if (userOptional.isPresent() && 
            userOptional.get().getPassword().equals(loginRequest.getPassword())) {
            
            User user = userOptional.get();
            
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
    
    // ==================== UTILITAIRES ====================
    
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