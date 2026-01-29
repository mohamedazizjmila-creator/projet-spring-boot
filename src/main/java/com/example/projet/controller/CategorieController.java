package com.example.projet.controller;

import com.example.projet.entity.Categorie;
import com.example.projet.service.CategorieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
//Ajoutez cette annotation en haut de chaque controller REST
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class CategorieController {
    
    @Autowired
    private CategorieService categorieService;
    
    @PostMapping
    public ResponseEntity<Categorie> createCategorie(@RequestBody Categorie categorie) {
        Categorie savedCategorie = categorieService.saveCategorie(categorie);
        return new ResponseEntity<>(savedCategorie, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<Categorie>> getAllCategories() {
        List<Categorie> categories = categorieService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Categorie> getCategorieById(@PathVariable Long id) {
        Optional<Categorie> categorie = categorieService.getCategorieById(id);
        if (categorie.isPresent()) {
            return ResponseEntity.ok(categorie.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Categorie> updateCategorie(@PathVariable Long id, @RequestBody Categorie categorieDetails) {
        Optional<Categorie> optionalCategorie = categorieService.getCategorieById(id);
        
        if (optionalCategorie.isPresent()) {
            Categorie existingCategorie = optionalCategorie.get();
            
            // Mettre à jour les champs
            if (categorieDetails.getNom() != null) {
                existingCategorie.setNom(categorieDetails.getNom());
            }
            if (categorieDetails.getDescription() != null) {
                existingCategorie.setDescription(categorieDetails.getDescription());
            }
            
            Categorie updatedCategorie = categorieService.saveCategorie(existingCategorie);
            return ResponseEntity.ok(updatedCategorie);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategorie(@PathVariable Long id) {
        Optional<Categorie> categorie = categorieService.getCategorieById(id);
        if (categorie.isPresent()) {
            categorieService.deleteCategorie(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Categorie>> searchCategories(@RequestParam String keyword) {
        List<Categorie> categories = categorieService.searchCategories(keyword);
        return ResponseEntity.ok(categories);
    }
}