package com.example.projet.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class Categorie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nom;
    
    private String description;
    
    @OneToMany(mappedBy = "categorie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"categorie", "hibernateLazyInitializer", "handler"})
    private List<Produit> produits = new ArrayList<>();
    
    // ============ CONSTRUCTEURS ============
    
    public Categorie() {
    }
    
    public Categorie(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }
    
    public Categorie(Long id, String nom, String description) {
        this.id = id;
        this.nom = nom;
        this.description = description;
    }
    
    public Categorie(Long id, String nom, String description, List<Produit> produits) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.produits = produits;
    }
    
    // ============ GETTERS ET SETTERS ============
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNom() {
        return nom;
    }
    
    public void setNom(String nom) {
        this.nom = nom;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<Produit> getProduits() {
        return produits;
    }
    
    public void setProduits(List<Produit> produits) {
        this.produits = produits;
    }
    
    // ============ MÉTHODES UTILITAIRES ============
    
    public void addProduit(Produit produit) {
        produits.add(produit);
        produit.setCategorie(this);
    }
    
    public void removeProduit(Produit produit) {
        produits.remove(produit);
        produit.setCategorie(null);
    }
    
    @Override
    public String toString() {
        return "Categorie{" +
               "id=" + id +
               ", nom='" + nom + '\'' +
               ", description='" + description + '\'' +
               ", nombreProduits=" + (produits != null ? produits.size() : 0) +
               '}';
    }
}