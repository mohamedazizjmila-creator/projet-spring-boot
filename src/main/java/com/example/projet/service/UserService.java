package com.example.projet.service;

import com.example.projet.entity.User;
import com.example.projet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // ==================== CRUD OPERATIONS ====================
    
    // Enregistrer ou mettre à jour un utilisateur
    public User save(User user) {
        if (user.getId() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    // Récupérer tous les utilisateurs
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    // Récupérer un utilisateur par ID
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    // Supprimer un utilisateur par ID
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
    
    // ==================== RECHERCHE ====================
    
    // Rechercher un utilisateur par nom d'utilisateur
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    // Rechercher un utilisateur par email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    // Rechercher les utilisateurs par rôle
    public List<User> findByRole(String role) {
        return userRepository.findByRole(role);
    }
    
    // ==================== VÉRIFICATIONS ====================
    
    // Vérifier si un nom d'utilisateur existe
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    // Vérifier si un email existe
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // Vérifier si un nom d'utilisateur existe pour un autre utilisateur
    public boolean usernameExistsForOtherUser(String username, Long excludedUserId) {
        return userRepository.existsByUsernameAndIdNot(username, excludedUserId);
    }
    
    // Vérifier si un email existe pour un autre utilisateur
    public boolean emailExistsForOtherUser(String email, Long excludedUserId) {
        return userRepository.existsByEmailAndIdNot(email, excludedUserId);
    }
    
    // ==================== AUTHENTIFICATION ====================
    
    // Authentifier un utilisateur
    public Optional<User> authenticate(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user;
        }
        return Optional.empty();
    }
    
    // ==================== MÉTHODES UTILES ====================
    
    // Compter le nombre d'utilisateurs
    public long count() {
        return userRepository.count();
    }
    
    // Compter le nombre d'utilisateurs par rôle
    public long countByRole(String role) {
        List<User> users = userRepository.findByRole(role);
        return users.size();
    }
    
    // Compter les utilisateurs actifs
    public long countActiveUsers() {
        List<User> activeUsers = userRepository.findByActiveTrue();
        return activeUsers.size();
    }
    
    // Compter les utilisateurs inactifs
    public long countInactiveUsers() {
        List<User> inactiveUsers = userRepository.findByActiveFalse();
        return inactiveUsers.size();
    }
    
    // Trouver tous les administrateurs
    public List<User> findAllAdmins() {
        return userRepository.findByRole("ADMIN");
    }
    
    // Trouver tous les utilisateurs normaux
    public List<User> findAllUsers() {
        return userRepository.findByRole("USER");
    }
}