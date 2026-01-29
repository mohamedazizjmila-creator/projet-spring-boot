package com.example.projet.service;

import com.example.projet.entity.*;
import com.example.projet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PanierItemService {
    
    @Autowired
    private PanierItemRepository panierItemRepository;
    
    @Autowired
    private PanierRepository panierRepository;
    
    @Autowired
    private ProduitRepository produitRepository;
    

    
    // ============ MÉTHODES DE RÉCUPÉRATION ============
    
    // Récupérer un item par ID
    public PanierItem getPanierItemById(Long itemId) {
        return panierItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item de panier non trouvé"));
    }
    
    // Récupérer tous les items d'un panier
    public List<PanierItem> getPanierItemsByPanierId(Long panierId) {
        return panierItemRepository.findByPanierId(panierId);
    }
    
    // Récupérer les items du panier d'un utilisateur
    public List<PanierItem> getPanierItemsByUserId(Long userId) {
        return panierItemRepository.findPanierItemsByUserId(userId);
    }
    
    // Récupérer un item spécifique par panier et produit
    public Optional<PanierItem> getPanierItem(Long panierId, Long produitId) {
        Panier panier = panierRepository.findById(panierId)
            .orElseThrow(() -> new RuntimeException("Panier non trouvé"));
        
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        return panierItemRepository.findByPanierAndProduit(panier, produit);
    }
    
    // Vérifier si un produit est dans le panier
    public boolean isProductInPanier(Long panierId, Long produitId) {
        Panier panier = panierRepository.findById(panierId)
            .orElseThrow(() -> new RuntimeException("Panier non trouvé"));
        
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        return panierItemRepository.existsByPanierAndProduit(panier, produit);
    }
    
    // Compter le nombre d'items dans un panier
    public Long countPanierItems(Long panierId) {
        return panierItemRepository.countByPanierId(panierId);
    }
    
    // Calculer le total du panier
    public Double calculatePanierTotal(Long panierId) {
        Double total = panierItemRepository.calculatePanierTotal(panierId);
        return total != null ? total : 0.0;
    }
    
    // ============ MÉTHODES D'ACTION ============
    
    // Ajouter un produit au panier
    @Transactional
    public PanierItem addToPanier(Long panierId, Long produitId, Integer quantite) {
        Panier panier = panierRepository.findById(panierId)
            .orElseThrow(() -> new RuntimeException("Panier non trouvé"));
        
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        // Vérifier le stock
        if (produit.getQuantite() < quantite) {
            throw new RuntimeException("Stock insuffisant. Disponible: " + produit.getQuantite());
        }
        
        // Vérifier si le produit est déjà dans le panier
        Optional<PanierItem> existingItem = panierItemRepository.findByPanierAndProduit(panier, produit);
        
        if (existingItem.isPresent()) {
            // Mettre à jour la quantité
            PanierItem item = existingItem.get();
            int nouvelleQuantite = item.getQuantite() + quantite;
            
            if (produit.getQuantite() < nouvelleQuantite) {
                throw new RuntimeException("Stock insuffisant. Disponible: " + produit.getQuantite());
            }
            
            item.setQuantite(nouvelleQuantite);
            return panierItemRepository.save(item);
        } else {
            // Créer un nouvel item
            PanierItem newItem = new PanierItem(panier, produit, quantite);
            return panierItemRepository.save(newItem);
        }
    }
    
    // Mettre à jour la quantité d'un item
    @Transactional
    public PanierItem updatePanierItemQuantite(Long itemId, Integer nouvelleQuantite) {
        PanierItem item = getPanierItemById(itemId);
        
        if (nouvelleQuantite <= 0) {
            // Supprimer l'item si quantité <= 0
            panierItemRepository.delete(item);
            return null;
        }
        
        // Vérifier le stock
        Produit produit = item.getProduit();
        if (produit.getQuantite() < nouvelleQuantite) {
            throw new RuntimeException("Stock insuffisant. Disponible: " + produit.getQuantite());
        }
        
        item.setQuantite(nouvelleQuantite);
        return panierItemRepository.save(item);
    }
    
    // Supprimer un item du panier
    @Transactional
    public void removeFromPanier(Long panierId, Long produitId) {
        Panier panier = panierRepository.findById(panierId)
            .orElseThrow(() -> new RuntimeException("Panier non trouvé"));
        
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        panierItemRepository.deleteByPanierAndProduit(panier, produit);
    }
    
    // Supprimer un item par ID
    @Transactional
    public void deletePanierItem(Long itemId) {
        PanierItem item = getPanierItemById(itemId);
        panierItemRepository.delete(item);
    }
    
    // Vider le panier
    @Transactional
    public void clearPanier(Long panierId) {
        panierItemRepository.deleteByPanierId(panierId);
    }
    
    // Vider le panier d'un utilisateur
    @Transactional
    public void clearUserPanier(Long userId) {
        Panier panier = panierRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Panier non trouvé"));
        
        panierItemRepository.deleteByPanier(panier);
    }
    
    // ============ MÉTHODES DE STATISTIQUES ============
    
    // Récupérer les produits les plus ajoutés aux paniers
    public List<Object[]> getMostAddedProducts() {
        return panierItemRepository.findMostAddedProducts();
    }
    
    // Calculer les détails complets du panier
    public PanierDetails getPanierDetails(Long panierId) {
        List<PanierItem> items = getPanierItemsByPanierId(panierId);
        Long itemCount = countPanierItems(panierId);
        Double total = calculatePanierTotal(panierId);
        
        return new PanierDetails(items, itemCount, total);
    }
    
    // Classe interne pour les détails du panier
    public static class PanierDetails {
        private List<PanierItem> items;
        private Long itemCount;
        private Double total;
        
        public PanierDetails(List<PanierItem> items, Long itemCount, Double total) {
            this.items = items;
            this.itemCount = itemCount;
            this.total = total;
        }
        
        // Getters
        public List<PanierItem> getItems() { return items; }
        public Long getItemCount() { return itemCount; }
        public Double getTotal() { return total; }
    }
}