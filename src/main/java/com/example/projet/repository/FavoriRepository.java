package com.example.projet.repository;

import com.example.projet.entity.Favori;
import com.example.projet.entity.User;
import com.example.projet.entity.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriRepository extends JpaRepository<Favori, Long> {
    List<Favori> findByUser(User user);
    List<Favori> findByUserId(Long userId);
    Optional<Favori> findByUserAndProduit(User user, Produit produit);
    boolean existsByUserAndProduit(User user, Produit produit);
    void deleteByUserAndProduit(User user, Produit produit);
}