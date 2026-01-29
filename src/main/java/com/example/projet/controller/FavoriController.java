package com.example.projet.controller;

import com.example.projet.entity.Favori;
import com.example.projet.service.FavoriService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favoris")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class FavoriController {
    
    @Autowired
    private FavoriService favoriService;
    
    // Récupérer les favoris de l'utilisateur
    @GetMapping("/mes-favoris")
    public ResponseEntity<?> getMesFavoris(@RequestParam Long userId) {
        try {
            List<Favori> favoris = favoriService.getUserFavoris(userId);
            return ResponseEntity.ok(favoris);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Ajouter aux favoris
    @PostMapping("/ajouter")
    public ResponseEntity<?> ajouterAuxFavoris(
            @RequestParam Long userId,
            @RequestParam Long produitId) {
        try {
            Favori favori = favoriService.addToFavoris(userId, produitId);
            return ResponseEntity.ok(favori);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Retirer des favoris
    @DeleteMapping("/retirer")
    public ResponseEntity<?> retirerDesFavoris(
            @RequestParam Long userId,
            @RequestParam Long produitId) {
        try {
            favoriService.removeFromFavoris(userId, produitId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Produit retiré des favoris");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Vérifier si un produit est dans les favoris
    @GetMapping("/verifier")
    public ResponseEntity<?> verifierFavori(
            @RequestParam Long userId,
            @RequestParam Long produitId) {
        try {
            boolean isFavori = favoriService.isProductInFavoris(userId, produitId);
            Map<String, Boolean> response = new HashMap<>();
            response.put("isFavori", isFavori);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}