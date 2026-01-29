package com.example.projet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "favoris")
public class Favori {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "produit_id")
    @JsonIgnoreProperties({"categorie", "hibernateLazyInitializer", "handler"})
    private Produit produit;
    
    @Column(name = "added_at")
    private LocalDateTime addedAt;
    
    // Constructeurs
    public Favori() {
        this.addedAt = LocalDateTime.now();
    }
    
    public Favori(User user, Produit produit) {
        this();
        this.user = user;
        this.produit = produit;
    }
    
    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }
    
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}