package com.example.projet.controller;

import com.example.projet.entity.Produit;
import com.example.projet.service.ProduitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import com.example.projet.service.FileStorageService;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/produits")
//Ajoutez cette annotation en haut de chaque controller REST
public class ProduitController {
    
    @Autowired
    private ProduitService produitService;
    @Autowired
    private FileStorageService fileStorageService;
    
    @PostMapping
    public ResponseEntity<Produit> createProduit(@RequestBody Produit produit) {
        Produit savedProduit = produitService.save(produit);
        return new ResponseEntity<>(savedProduit, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<Produit>> getAllProduits() {
        List<Produit> produits = produitService.findAll();
        return ResponseEntity.ok(produits);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Produit> getProduitById(@PathVariable Long id) {
        Optional<Produit> produit = produitService.findById(id);
        if (produit.isPresent()) {
            return ResponseEntity.ok(produit.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    // AJOUTE CETTE MÉTHODE
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImageForProduct(
            @RequestParam("productId") Long productId,
            @RequestParam("image") MultipartFile file) {
        
        System.out.println("🚀 Upload image appelé pour produit ID: " + productId);
        
        try {
            // 1. Vérifie le fichier
            if (file.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Fichier vide");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 2. Sauvegarde l'image
            String fileName = fileStorageService.saveImage(file);
            System.out.println("✅ Image sauvegardée: " + fileName);
            
            // 3. Mets à jour le produit dans la base
            Optional<Produit> produitOpt = produitService.findById(productId);
            if (produitOpt.isPresent()) {
                Produit produit = produitOpt.get();
                String imageUrl = "/uploads/" + fileName;
                produit.setImageUrl(imageUrl);
                produitService.save(produit);
                System.out.println("✅ Produit mis à jour avec image: " + imageUrl);
            }
            
            // 4. Retourne la réponse
            String fullUrl = "https://projet-api-v2.onrender.com/images/" + fileName;
            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("imageUrl", fullUrl);
            response.put("fileName", fileName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

// NE CHANGE PAS ICI - Ton fichier se termine ici
    
    @PutMapping("/{id}")
    public ResponseEntity<Produit> updateProduit(@PathVariable Long id, @RequestBody Produit produitDetails) {
        Optional<Produit> optionalProduit = produitService.findById(id);
        
        if (optionalProduit.isPresent()) {
            Produit existingProduit = optionalProduit.get();
            
            // Mettre à jour les champs
            if (produitDetails.getNom() != null) {
                existingProduit.setNom(produitDetails.getNom());
            }
            if (produitDetails.getDescription() != null) {
                existingProduit.setDescription(produitDetails.getDescription());
            }
            if (produitDetails.getPrix() != null) {
                existingProduit.setPrix(produitDetails.getPrix());
            }
            if (produitDetails.getQuantite() != null) {
                existingProduit.setQuantite(produitDetails.getQuantite());
            }
            if (produitDetails.getCategorie() != null) {
                existingProduit.setCategorie(produitDetails.getCategorie());
            }
            
            Produit updatedProduit = produitService.save(existingProduit);
            return ResponseEntity.ok(updatedProduit);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        Optional<Produit> produit = produitService.findById(id);
        if (produit.isPresent()) {
            produitService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Produit>> searchProduits(@RequestParam String keyword) {
        List<Produit> produits = produitService.findByNomContaining(keyword);
        return ResponseEntity.ok(produits);
    }
    
    @GetMapping("/categorie/{categorieId}")
    public ResponseEntity<List<Produit>> getProduitsByCategorie(@PathVariable Long categorieId) {
        List<Produit> produits = produitService.findByCategorieId(categorieId);
        return ResponseEntity.ok(produits);
    }
    
    @GetMapping("/price-range")
    public ResponseEntity<List<Produit>> getProduitsByPriceRange(
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max) {
        
        List<Produit> produits = produitService.findByPrixBetween(min, max);
        return ResponseEntity.ok(produits);
    }
    
    @GetMapping("/in-stock")
    public ResponseEntity<List<Produit>> getProduitsInStock() {
        List<Produit> produits = produitService.findProduitsEnStock();
        return ResponseEntity.ok(produits);
    }
    
    @PostMapping("/{id}/augmenter-quantite")
    public ResponseEntity<Produit> augmenterQuantite(@PathVariable Long id, @RequestParam Integer quantite) {
        Produit produit = produitService.augmenterQuantite(id, quantite);
        if (produit != null) {
            return ResponseEntity.ok(produit);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/diminuer-quantite")
    public ResponseEntity<Produit> diminuerQuantite(@PathVariable Long id, @RequestParam Integer quantite) {
        Produit produit = produitService.diminuerQuantite(id, quantite);
        if (produit != null) {
            return ResponseEntity.ok(produit);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/count")
    public ResponseEntity<Long> countProduits() {
        long count = produitService.count();
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/count-in-stock")
    public ResponseEntity<Long> countProduitsInStock() {
        long count = produitService.countEnStock();
        return ResponseEntity.ok(count);
    }
    
}