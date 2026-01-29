package com.example.projet.service;

import com.example.projet.entity.*;
import com.example.projet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FavoriService {
    
    @Autowired
    private FavoriRepository favoriRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProduitRepository produitRepository;
    
    // Récupérer les favoris d'un utilisateur
    public List<Favori> getUserFavoris(Long userId) {
        return favoriRepository.findByUserId(userId);
    }
    
    // Ajouter un produit aux favoris
    @Transactional
    public Favori addToFavoris(Long userId, Long produitId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        // Vérifier si le produit est déjà dans les favoris
        if (favoriRepository.existsByUserAndProduit(user, produit)) {
            throw new RuntimeException("Ce produit est déjà dans vos favoris");
        }
        
        Favori favori = new Favori(user, produit);
        return favoriRepository.save(favori);
    }
    
    // Retirer un produit des favoris
    @Transactional
    public void removeFromFavoris(Long userId, Long produitId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        favoriRepository.deleteByUserAndProduit(user, produit);
    }
    
    // Vérifier si un produit est dans les favoris
    public boolean isProductInFavoris(Long userId, Long produitId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        return favoriRepository.existsByUserAndProduit(user, produit);
    }
    
    // Retirer tous les favoris d'un utilisateur
    @Transactional
    public void clearFavoris(Long userId) {
        List<Favori> favoris = favoriRepository.findByUserId(userId);
        favoriRepository.deleteAll(favoris);
    }
}