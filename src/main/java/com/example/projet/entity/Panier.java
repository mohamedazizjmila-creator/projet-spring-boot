package com.example.projet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paniers")
public class Panier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @OneToMany(mappedBy = "panier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PanierItem> items = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructeurs
    public Panier() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Panier(User user) {
        this();
        this.user = user;
    }
    
    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public List<PanierItem> getItems() { return items; }
    public void setItems(List<PanierItem> items) { this.items = items; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Méthodes utilitaires
    public void addItem(PanierItem item) {
        items.add(item);
        item.setPanier(this);
        updatedAt = LocalDateTime.now();
    }
    
    public void removeItem(PanierItem item) {
        items.remove(item);
        item.setPanier(null);
        updatedAt = LocalDateTime.now();
    }
    
    public Double getTotal() {
        return items.stream()
            .mapToDouble(item -> item.getProduit().getPrix() * item.getQuantite())
            .sum();
    }
    
    public Integer getTotalItems() {
        return items.stream()
            .mapToInt(PanierItem::getQuantite)
            .sum();
    }
}