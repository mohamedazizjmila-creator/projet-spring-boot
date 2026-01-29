package com.example.projet.repository;

import com.example.projet.entity.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long> {
    List<Categorie> findByNomContaining(String nom);
    Optional<Categorie> findByNom(String nom);
   
}