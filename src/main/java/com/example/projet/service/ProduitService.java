package com.example.projet.service;

import com.example.projet.entity.Produit;
import com.example.projet.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProduitService {
    
    @Autowired
    private ProduitRepository produitRepository;
    
    // Enregistrer un produit
    public Produit save(Produit produit) {
        return produitRepository.save(produit);
    }
    
    // Récupérer tous les produits
    public List<Produit> findAll() {
        return produitRepository.findAll();
    }
    
    // Récupérer un produit par ID
    public Optional<Produit> findById(Long id) {
        return produitRepository.findById(id);
    }
    
    // Supprimer un produit par ID
    public void deleteById(Long id) {
        produitRepository.deleteById(id);
    }
    
    // Rechercher des produits par nom
    public List<Produit> findByNomContaining(String nom) {
        return produitRepository.findByNomContaining(nom);
    }
    
    // Rechercher des produits par catégorie
    public List<Produit> findByCategorieId(Long categorieId) {
        return produitRepository.findAll()
                .stream()
                .filter(p -> p.getCategorie() != null && 
                           p.getCategorie().getId() != null && 
                           p.getCategorie().getId().equals(categorieId))
                .toList();
    }
    
    // Rechercher des produits par plage de prix
    public List<Produit> findByPrixBetween(Double min, Double max) {
        return produitRepository.findAll()
                .stream()
                .filter(p -> {
                    if (p.getPrix() == null) return false;
                    boolean matchesMin = min == null || p.getPrix() >= min;
                    boolean matchesMax = max == null || p.getPrix() <= max;
                    return matchesMin && matchesMax;
                })
                .toList();
    }
    
    // Récupérer les produits en stock
    public List<Produit> findProduitsEnStock() {
        return produitRepository.findAll()
                .stream()
                .filter(p -> p.getQuantite() != null && p.getQuantite() > 0)
                .toList();
    }
    
    // Récupérer les produits avec faible stock
    public List<Produit> findProduitsFaibleStock(Integer seuil) {
        return produitRepository.findAll()
                .stream()
                .filter(p -> p.getQuantite() != null && 
                           p.getQuantite() > 0 && 
                           p.getQuantite() <= seuil)
                .toList();
    }
    
    // Mettre à jour la quantité d'un produit
    public Produit updateQuantite(Long id, Integer nouvelleQuantite) {
        Optional<Produit> produitOptional = produitRepository.findById(id);
        if (produitOptional.isPresent()) {
            Produit produit = produitOptional.get();
            produit.setQuantite(nouvelleQuantite);
            return produitRepository.save(produit);
        }
        return null;
    }
    
    // Augmenter la quantité d'un produit
    public Produit augmenterQuantite(Long id, Integer quantiteAjoutee) {
        Optional<Produit> produitOptional = produitRepository.findById(id);
        if (produitOptional.isPresent()) {
            Produit produit = produitOptional.get();
            Integer quantiteActuelle = produit.getQuantite() != null ? produit.getQuantite() : 0;
            produit.setQuantite(quantiteActuelle + quantiteAjoutee);
            return produitRepository.save(produit);
        }
        return null;
    }
    
    // Diminuer la quantité d'un produit
    public Produit diminuerQuantite(Long id, Integer quantiteRetiree) {
        Optional<Produit> produitOptional = produitRepository.findById(id);
        if (produitOptional.isPresent()) {
            Produit produit = produitOptional.get();
            Integer quantiteActuelle = produit.getQuantite() != null ? produit.getQuantite() : 0;
            Integer nouvelleQuantite = Math.max(0, quantiteActuelle - quantiteRetiree);
            produit.setQuantite(nouvelleQuantite);
            return produitRepository.save(produit);
        }
        return null;
    }
    
    // Compter le nombre de produits
    public long count() {
        return produitRepository.count();
    }
    
    // Compter les produits en stock
    public long countEnStock() {
        return produitRepository.findAll()
                .stream()
                .filter(p -> p.getQuantite() != null && p.getQuantite() > 0)
                .count();
    }
}