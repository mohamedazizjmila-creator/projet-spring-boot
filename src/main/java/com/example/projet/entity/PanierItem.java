package com.example.projet.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "panier_items")
public class PanierItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "panier_id")
    @JsonIgnoreProperties({"items", "hibernateLazyInitializer", "handler"})
    private Panier panier;
    
    @ManyToOne
    @JoinColumn(name = "produit_id")
    @JsonIgnoreProperties({"categorie", "hibernateLazyInitializer", "handler"})
    private Produit produit;
    
    @Column(nullable = false)
    private Integer quantite;
    
    @Column(name = "added_at")
    private LocalDateTime addedAt;
    
    // Constructeurs
    public PanierItem() {
        this.addedAt = LocalDateTime.now();
    }
    
    public PanierItem(Panier panier, Produit produit, Integer quantite) {
        this();
        this.panier = panier;
        this.produit = produit;
        this.quantite = quantite;
    }
    
    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Panier getPanier() { return panier; }
    public void setPanier(Panier panier) { this.panier = panier; }
    
    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }
    
    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }
    
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
    
    // Méthode utilitaire
    public Double getSousTotal() {
        return produit.getPrix() * quantite;
    }
}