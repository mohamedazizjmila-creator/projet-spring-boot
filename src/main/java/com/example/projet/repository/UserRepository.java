package com.example.projet.repository;

import com.example.projet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Rechercher un utilisateur par nom d'utilisateur
    Optional<User> findByUsername(String username);
    
    // Rechercher un utilisateur par email
    Optional<User> findByEmail(String email);
    
    // Vérifier si un nom d'utilisateur existe déjà
    boolean existsByUsername(String username);
    
    // Vérifier si un email existe déjà
    boolean existsByEmail(String email);
    
    // Rechercher les utilisateurs par rôle
    List<User> findByRole(String role);
    
    // Rechercher les utilisateurs actifs
    List<User> findByActiveTrue();
    
    // Rechercher les utilisateurs inactifs
    List<User> findByActiveFalse();
    
    // Vérifier si un username existe pour un autre utilisateur
    boolean existsByUsernameAndIdNot(String username, Long id);
    
    // Vérifier si un email existe pour un autre utilisateur
    boolean existsByEmailAndIdNot(String email, Long id);
}