package com.example.projet.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "order_items")
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnoreProperties({"items", "hibernateLazyInitializer", "handler"})
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "produit_id")
    @JsonIgnoreProperties({"categorie", "hibernateLazyInitializer", "handler"})
    private Produit produit;
    
    @Column(nullable = false)
    private Integer quantite;
    
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;
    
    @Column(name = "subtotal", nullable = false)
    private Double subtotal;
    
    // Constructeurs
    public OrderItem() {}
    
    public OrderItem(Order order, Produit produit, Integer quantite) {
        this.order = order;
        this.produit = produit;
        this.quantite = quantite;
        this.unitPrice = produit.getPrix();
        this.subtotal = unitPrice * quantite;
    }
    
    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    
    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { 
        this.produit = produit;
        this.unitPrice = produit.getPrix();
        calculateSubtotal();
    }
    
    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { 
        this.quantite = quantite;
        calculateSubtotal();
    }
    
    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { 
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }
    
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    
    // Méthode utilitaire
    private void calculateSubtotal() {
        if (unitPrice != null && quantite != null) {
            this.subtotal = unitPrice * quantite;
        }
    }
}