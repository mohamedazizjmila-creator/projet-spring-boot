package com.example.projet.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "produits")
public class Produit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nom;
    
    private String description;
    private Double prix;
    private Integer quantite;
    
    @Column(name = "image_name")
    private String imageName;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categorie_id")
    @JsonIgnoreProperties({"produits", "hibernateLazyInitializer", "handler"})
    private Categorie categorie;
    
    // ============ CONSTRUCTEURS ============
    
    public Produit() {
    }
    
    public Produit(String nom, String description, Double prix, Integer quantite, 
                   String imageName, String imageUrl, Categorie categorie) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.quantite = quantite;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.categorie = categorie;
    }
    
    public Produit(String nom, String description, Double prix, Integer quantite, Categorie categorie) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.quantite = quantite;
        this.categorie = categorie;
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
    
    public Double getPrix() {
        return prix;
    }
    
    public void setPrix(Double prix) {
        this.prix = prix;
    }
    
    public Integer getQuantite() {
        return quantite;
    }
    
    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }
    
    public String getImageName() {
        return imageName;
    }
    
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public Categorie getCategorie() {
        return categorie;
    }
    
    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }
    
    // ============ MÉTHODES UTILES ============
    
    public String getFullImageUrl() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        return imageUrl.startsWith("/") ? imageUrl : "/" + imageUrl;
    }
    
    @Override
    public String toString() {
        return "Produit{" +
               "id=" + id +
               ", nom='" + nom + '\'' +
               ", description='" + description + '\'' +
               ", prix=" + prix +
               ", quantite=" + quantite +
               ", imageName='" + imageName + '\'' +
               ", imageUrl='" + imageUrl + '\'' +
               ", categorie=" + (categorie != null ? categorie.getNom() : "null") +
               '}';
    }
}