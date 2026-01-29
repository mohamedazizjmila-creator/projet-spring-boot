package com.example.projet.controller;

import com.example.projet.entity.Panier;
import com.example.projet.service.PanierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/panier")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class PanierController {
    
    @Autowired
    private PanierService panierService;
    
    // Récupérer le panier de l'utilisateur
    @GetMapping("/mon-panier")
    public ResponseEntity<?> getMonPanier(@RequestParam Long userId) {
        try {
            Panier panier = panierService.getPanierByUserId(userId);
            return ResponseEntity.ok(panier);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Ajouter au panier
    @PostMapping("/ajouter")
    public ResponseEntity<?> ajouterAuPanier(
            @RequestParam Long userId,
            @RequestParam Long produitId,
            @RequestParam(defaultValue = "1") Integer quantite) {
        
        System.out.println("========================");
        System.out.println("📦 POST /api/panier/ajouter");
        System.out.println("👤 userId: " + userId);
        System.out.println("🛍️ produitId: " + produitId);
        System.out.println("🔢 quantite: " + quantite);
        System.out.println("========================");
        
        try {
            Panier panier = panierService.addToPanier(userId, produitId, quantite);
            System.out.println("✅ Produit ajouté avec succès");
            return ResponseEntity.ok(panier);
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Mettre à jour la quantité
    @PutMapping("/update")
    public ResponseEntity<?> updateQuantite(
            @RequestParam Long userId,
            @RequestParam Long produitId,
            @RequestParam Integer quantite) {
        try {
            Panier panier = panierService.updatePanierItem(userId, produitId, quantite);
            return ResponseEntity.ok(panier);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Retirer du panier
    @DeleteMapping("/retirer")
    public ResponseEntity<?> retirerDuPanier(
            @RequestParam Long userId,
            @RequestParam Long produitId) {
        try {
            Panier panier = panierService.removeFromPanier(userId, produitId);
            return ResponseEntity.ok(panier);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Vider le panier
    @DeleteMapping("/vider")
    public ResponseEntity<?> viderPanier(@RequestParam Long userId) {
        try {
            panierService.clearPanier(userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Panier vidé avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}