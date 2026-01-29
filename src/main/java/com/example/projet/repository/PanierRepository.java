package com.example.projet.repository;

import com.example.projet.entity.Panier;
import com.example.projet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PanierRepository extends JpaRepository<Panier, Long> {
    Optional<Panier> findByUser(User user);
    Optional<Panier> findByUserId(Long userId);
    void deleteByUser(User user);
}