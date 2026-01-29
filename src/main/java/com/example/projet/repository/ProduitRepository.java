package com.example.projet.repository;

import com.example.projet.entity.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    List<Produit> findByNomContaining(String nom);
    List<Produit> findByCategorieId(Long categorieId);
    List<Produit> findByPrixBetween(Double min, Double max);
    List<Produit> findByQuantiteGreaterThan(Integer quantite);
    Optional<Produit> findByNom(String nom);
}