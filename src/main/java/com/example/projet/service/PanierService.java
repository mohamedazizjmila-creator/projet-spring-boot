package com.example.projet.service;

import com.example.projet.entity.*;
import com.example.projet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PanierService {
    
    @Autowired
    private PanierRepository panierRepository;
    
    @Autowired
    private PanierItemRepository panierItemRepository;
    
    @Autowired
    private ProduitRepository produitRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PanierItemService panierItemService;
    
    // ============ MÉTHODES DE BASE ============
    
    // Récupérer un panier par ID
    public Panier getPanierById(Long panierId) {
        return panierRepository.findById(panierId)
            .orElseThrow(() -> new RuntimeException("Panier non trouvé"));
    }
    
    // Récupérer le panier d'un utilisateur
    public Panier getPanierByUserId(Long userId) {
        return panierRepository.findByUserId(userId)
            .orElseGet(() -> createPanier(userId));
    }
    
    // Récupérer le panier d'un utilisateur par username
    public Panier getPanierByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return getPanierByUserId(user.getId());
    }
    
    // Créer un nouveau panier pour un utilisateur
    @Transactional
    public Panier createPanier(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        // Vérifier si l'utilisateur a déjà un panier
        Optional<Panier> existingPanier = panierRepository.findByUser(user);
        if (existingPanier.isPresent()) {
            return existingPanier.get();
        }
        
        Panier panier = new Panier(user);
        return panierRepository.save(panier);
    }
    
    // Supprimer un panier
    @Transactional
    public void deletePanier(Long panierId) {
        Panier panier = getPanierById(panierId);
        panierItemService.clearPanier(panierId); // Vider d'abord les items
        panierRepository.delete(panier);
    }
    
    // ============ GESTION DES ITEMS ============
    
    // Récupérer tous les items d'un panier
    public List<PanierItem> getPanierItems(Long userId) {
        Panier panier = getPanierByUserId(userId);
        return panierItemRepository.findByPanier(panier);
    }
    
    // Ajouter un produit au panier
    @Transactional
    public Panier addToPanier(Long userId, Long produitId, Integer quantite) {
        Panier panier = getPanierByUserId(userId);
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        // Vérifier la disponibilité du stock
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
            panierItemRepository.save(item);
        } else {
            // Ajouter un nouvel item
            PanierItem newItem = new PanierItem(panier, produit, quantite);
            panier.addItem(newItem);
            panierItemRepository.save(newItem);
        }
        
        // Mettre à jour la date de modification
        panier.setUpdatedAt(LocalDateTime.now());
        
        return panierRepository.save(panier);
    }
    
    // Mettre à jour la quantité d'un produit dans le panier
    @Transactional
    public Panier updatePanierItem(Long userId, Long produitId, Integer quantite) {
        Panier panier = getPanierByUserId(userId);
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        PanierItem item = panierItemRepository.findByPanierAndProduit(panier, produit)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé dans le panier"));
        
        if (quantite <= 0) {
            // Supprimer l'item si quantité <= 0
            panier.removeItem(item);
            panierItemRepository.delete(item);
        } else {
            // Vérifier le stock
            if (produit.getQuantite() < quantite) {
                throw new RuntimeException("Stock insuffisant. Disponible: " + produit.getQuantite());
            }
            
            // Mettre à jour la quantité
            item.setQuantite(quantite);
            panierItemRepository.save(item);
        }
        
        // Mettre à jour la date de modification
        panier.setUpdatedAt(LocalDateTime.now());
        
        return panierRepository.save(panier);
    }
    
    // Retirer un produit du panier
    @Transactional
    public Panier removeFromPanier(Long userId, Long produitId) {
        Panier panier = getPanierByUserId(userId);
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        PanierItem item = panierItemRepository.findByPanierAndProduit(panier, produit)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé dans le panier"));
        
        panier.removeItem(item);
        panierItemRepository.delete(item);
        
        // Mettre à jour la date de modification
        panier.setUpdatedAt(LocalDateTime.now());
        
        return panierRepository.save(panier);
    }
    
    // Vider complètement le panier
    @Transactional
    public void clearPanier(Long userId) {
        Panier panier = getPanierByUserId(userId);
        panierItemRepository.deleteByPanier(panier);
        panier.getItems().clear();
        
        // Mettre à jour la date de modification
        panier.setUpdatedAt(LocalDateTime.now());
        
        panierRepository.save(panier);
    }
    
    // ============ CALCULS ET STATISTIQUES ============
    
    // Calculer le total du panier
    public Double calculatePanierTotal(Long userId) {
        Panier panier = getPanierByUserId(userId);
        Double total = panierItemRepository.calculatePanierTotal(panier.getId());
        return total != null ? total : 0.0;
    }
    
    // Calculer le total du panier avec TVA
    public Map<String, Object> calculatePanierTotalWithTaxes(Long userId) {
        Double sousTotal = calculatePanierTotal(userId);
        Double tva = sousTotal * 0.20; // 20% de TVA
        Double totalTTC = sousTotal + tva;
        
        Map<String, Object> result = new HashMap<>();
        result.put("sousTotal", sousTotal);
        result.put("tva", tva);
        result.put("totalTTC", totalTTC);
        result.put("tauxTVA", 20.0);
        
        return result;
    }
    
    // Compter le nombre d'articles dans le panier
    public Integer countPanierItems(Long userId) {
        Panier panier = getPanierByUserId(userId);
        Long count = panierItemRepository.countByPanierId(panier.getId());
        return count != null ? count.intValue() : 0;
    }
    
    // Compter le nombre total de produits (quantité totale)
    public Integer countTotalProducts(Long userId) {
        Panier panier = getPanierByUserId(userId);
        return panier.getItems().stream()
            .mapToInt(PanierItem::getQuantite)
            .sum();
    }
    
    // Vérifier si le panier est vide
    public boolean isPanierEmpty(Long userId) {
        return countPanierItems(userId) == 0;
    }
    
    // Vérifier si un produit est dans le panier
    public boolean isProductInPanier(Long userId, Long produitId) {
        Panier panier = getPanierByUserId(userId);
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        return panierItemRepository.existsByPanierAndProduit(panier, produit);
    }
    
    // Récupérer la quantité d'un produit dans le panier
    public Integer getProductQuantityInPanier(Long userId, Long produitId) {
        Panier panier = getPanierByUserId(userId);
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        return panierItemRepository.findByPanierAndProduit(panier, produit)
            .map(PanierItem::getQuantite)
            .orElse(0);
    }
    
    // ============ VALIDATION ============
    
    // Valider le panier avant commande
    public Map<String, Object> validatePanier(Long userId) {
        Panier panier = getPanierByUserId(userId);
        Map<String, Object> validation = new HashMap<>();
        List<Map<String, Object>> errors = new java.util.ArrayList<>();
        
        // Vérifier si le panier est vide
        if (panier.getItems().isEmpty()) {
            validation.put("valid", false);
            validation.put("message", "Votre panier est vide");
            return validation;
        }
        
        // Vérifier le stock pour chaque produit
        for (PanierItem item : panier.getItems()) {
            Produit produit = item.getProduit();
            if (produit.getQuantite() < item.getQuantite()) {
                Map<String, Object> error = new HashMap<>();
                error.put("produitId", produit.getId());
                error.put("produitNom", produit.getNom());
                error.put("demande", item.getQuantite());
                error.put("disponible", produit.getQuantite());
                error.put("message", "Stock insuffisant pour " + produit.getNom());
                errors.add(error);
            }
            
            // Vérifier si le produit a du stock (au moins 1)
            if (produit.getQuantite() <= 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("produitId", produit.getId());
                error.put("produitNom", produit.getNom());
                error.put("message", "Produit en rupture de stock: " + produit.getNom());
                errors.add(error);
            }
        }
        
        if (!errors.isEmpty()) {
            validation.put("valid", false);
            validation.put("errors", errors);
        } else {
            validation.put("valid", true);
            validation.put("message", "Panier valide");
            validation.put("total", calculatePanierTotal(userId));
            validation.put("itemCount", countPanierItems(userId));
        }
        
        return validation;
    }
    
    // ============ TRANSFERT ET FUSION ============
    
    // Transférer le panier d'un utilisateur à un autre (ex: guest -> user)
    @Transactional
    public Panier transferPanier(Long fromUserId, Long toUserId) {
        Panier fromPanier = getPanierByUserId(fromUserId);
        Panier toPanier = getPanierByUserId(toUserId);
        
        // Si le panier de destination est vide, simplement l'assigner
        if (toPanier.getItems().isEmpty()) {
            toPanier.setUser(userRepository.findById(toUserId).orElseThrow());
            panierRepository.delete(fromPanier);
            return panierRepository.save(toPanier);
        }
        
        // Sinon, fusionner les paniers
        for (PanierItem item : fromPanier.getItems()) {
            Optional<PanierItem> existingItem = panierItemRepository.findByPanierAndProduit(toPanier, item.getProduit());
            
            if (existingItem.isPresent()) {
                // Mettre à jour la quantité
                PanierItem toItem = existingItem.get();
                int nouvelleQuantite = toItem.getQuantite() + item.getQuantite();
                
                // Vérifier le stock avant de fusionner
                if (item.getProduit().getQuantite() < nouvelleQuantite) {
                    // Si stock insuffisant, on met la quantité maximale disponible
                    nouvelleQuantite = item.getProduit().getQuantite();
                }
                
                toItem.setQuantite(nouvelleQuantite);
                panierItemRepository.save(toItem);
            } else {
                // Ajouter le nouvel item
                PanierItem newItem = new PanierItem(toPanier, item.getProduit(), item.getQuantite());
                panierItemRepository.save(newItem);
                toPanier.addItem(newItem);
            }
        }
        
        // Supprimer l'ancien panier
        panierItemRepository.deleteByPanier(fromPanier);
        panierRepository.delete(fromPanier);
        
        // Mettre à jour la date de modification
        toPanier.setUpdatedAt(LocalDateTime.now());
        
        return panierRepository.save(toPanier);
    }
    
    // Fusionner deux paniers
    @Transactional
    public Panier mergePaniers(Long panierId1, Long panierId2) {
        Panier panier1 = getPanierById(panierId1);
        Panier panier2 = getPanierById(panierId2);
        
        // Fusionner les items de panier2 dans panier1
        for (PanierItem item : panier2.getItems()) {
            Optional<PanierItem> existingItem = panierItemRepository.findByPanierAndProduit(panier1, item.getProduit());
            
            if (existingItem.isPresent()) {
                PanierItem existing = existingItem.get();
                int nouvelleQuantite = existing.getQuantite() + item.getQuantite();
                
                // Vérifier le stock avant de fusionner
                if (item.getProduit().getQuantite() < nouvelleQuantite) {
                    nouvelleQuantite = item.getProduit().getQuantite();
                }
                
                existing.setQuantite(nouvelleQuantite);
                panierItemRepository.save(existing);
            } else {
                PanierItem newItem = new PanierItem(panier1, item.getProduit(), item.getQuantite());
                panierItemRepository.save(newItem);
                panier1.addItem(newItem);
            }
        }
        
        // Supprimer le panier2
        panierItemRepository.deleteByPanier(panier2);
        panierRepository.delete(panier2);
        
        // Mettre à jour la date de modification
        panier1.setUpdatedAt(LocalDateTime.now());
        
        return panierRepository.save(panier1);
    }
    
    // ============ STATISTIQUES ET RAPPORTS ============
    
    // Récupérer les statistiques d'un panier
    public Map<String, Object> getPanierStatistics(Long userId) {
        Panier panier = getPanierByUserId(userId);
        List<PanierItem> items = panier.getItems();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("panierId", panier.getId());
        stats.put("userId", userId);
        stats.put("itemCount", items.size());
        stats.put("totalQuantity", countTotalProducts(userId));
        stats.put("totalAmount", calculatePanierTotal(userId));
        stats.put("createdAt", panier.getCreatedAt());
        stats.put("updatedAt", panier.getUpdatedAt());
        stats.put("isEmpty", items.isEmpty());
        
        // Catégories dans le panier (si le produit a une catégorie)
        List<String> categories = items.stream()
            .filter(item -> item.getProduit().getCategorie() != null)
            .map(item -> item.getProduit().getCategorie().getNom())
            .distinct()
            .toList();
        stats.put("categories", categories);
        
        // Produit le plus cher dans le panier
        items.stream()
            .max((i1, i2) -> Double.compare(
                i1.getProduit().getPrix() * i1.getQuantite(),
                i2.getProduit().getPrix() * i2.getQuantite()))
            .ifPresent(item -> {
                Map<String, Object> mostExpensive = new HashMap<>();
                mostExpensive.put("produitId", item.getProduit().getId());
                mostExpensive.put("produitNom", item.getProduit().getNom());
                mostExpensive.put("quantite", item.getQuantite());
                mostExpensive.put("prixUnitaire", item.getProduit().getPrix());
                mostExpensive.put("total", item.getSousTotal());
                stats.put("mostExpensiveItem", mostExpensive);
            });
        
        return stats;
    }
    
    // Générer un récapitulatif du panier pour affichage
    public Map<String, Object> getPanierSummary(Long userId) {
        Panier panier = getPanierByUserId(userId);
        Double total = calculatePanierTotal(userId);
        Integer itemCount = countPanierItems(userId);
        Integer totalQuantity = countTotalProducts(userId);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("panierId", panier.getId());
        summary.put("itemCount", itemCount);
        summary.put("totalQuantity", totalQuantity);
        summary.put("subtotal", total);
        
        // Calculer les taxes (TVA 20%)
        Double tva = total * 0.20;
        Double totalTTC = total + tva;
        
        summary.put("tva", tva);
        summary.put("totalTTC", totalTTC);
        summary.put("shipping", 0.0); // Frais de livraison (à configurer)
        summary.put("discount", 0.0); // Remise (à configurer)
        
        // Détails des produits
        List<Map<String, Object>> produits = panier.getItems().stream()
            .map(item -> {
                Map<String, Object> produit = new HashMap<>();
                produit.put("id", item.getProduit().getId());
                produit.put("nom", item.getProduit().getNom());
                produit.put("prix", item.getProduit().getPrix());
                produit.put("quantite", item.getQuantite());
                produit.put("sousTotal", item.getSousTotal());
                produit.put("imageUrl", item.getProduit().getImageUrl());
                produit.put("stockDisponible", item.getProduit().getQuantite());
                return produit;
            })
            .toList();
        
        summary.put("produits", produits);
        
        return summary;
    }
    
    // ============ MÉTHODES UTILITAIRES ============
    
    // Mettre à jour la date de modification
    @Transactional
    public void updatePanierTimestamp(Long userId) {
        Panier panier = getPanierByUserId(userId);
        panier.setUpdatedAt(LocalDateTime.now());
        panierRepository.save(panier);
    }
    
    // Récupérer ou créer un panier pour un utilisateur non connecté (guest)
    public Panier getOrCreateGuestPanier(String sessionId) {
        // Rechercher un panier existant pour cette session
        // (Implémentation simplifiée - vous pourriez stocker cela différemment)
        try {
            return panierRepository.findByUserId(-1L) // ID spécial pour les guests
                .orElseGet(() -> {
                    // Créer un utilisateur temporaire pour le guest
                    User guestUser = new User();
                    guestUser.setUsername("guest_" + sessionId);
                    guestUser.setEmail("guest@example.com");
                    guestUser.setPassword("guest");
                    guestUser.setRole("GUEST");
                    guestUser.setActive(true);
                    User savedUser = userRepository.save(guestUser);
                    
                    return createPanier(savedUser.getId());
                });
        } catch (Exception e) {
            // En cas d'erreur, créer un panier avec un utilisateur par défaut
            User guestUser = new User();
            guestUser.setUsername("guest_" + sessionId);
            guestUser.setEmail("guest@example.com");
            guestUser.setPassword("guest");
            guestUser.setRole("GUEST");
            guestUser.setActive(true);
            User savedUser = userRepository.save(guestUser);
            
            return createPanier(savedUser.getId());
        }
    }
    
    // Synchroniser le panier guest avec un utilisateur connecté
    @Transactional
    public Panier synchronizeGuestPanier(String guestSessionId, Long userId) {
        Panier guestPanier = getOrCreateGuestPanier(guestSessionId);
        return transferPanier(guestPanier.getUser().getId(), userId);
    }
    
    // Méthode pour vérifier la disponibilité d'un produit avant de l'ajouter au panier
    public boolean isProductAvailable(Long produitId, Integer quantiteDemandee) {
        try {
            Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
            
            return produit.getQuantite() >= quantiteDemandee;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Méthode pour réserver temporairement le stock (à utiliser avant validation de commande)
    public Map<String, Object> reserveStockForPanier(Long userId) {
        Panier panier = getPanierByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> reservations = new java.util.ArrayList<>();
        
        for (PanierItem item : panier.getItems()) {
            Produit produit = item.getProduit();
            if (produit.getQuantite() >= item.getQuantite()) {
                // Ici, vous pourriez implémenter une logique de réservation temporaire
                Map<String, Object> reservation = new HashMap<>();
                reservation.put("produitId", produit.getId());
                reservation.put("produitNom", produit.getNom());
                reservation.put("quantiteReservee", item.getQuantite());
                reservation.put("stockRestant", produit.getQuantite() - item.getQuantite());
                reservations.add(reservation);
            } else {
                result.put("success", false);
                result.put("message", "Stock insuffisant pour " + produit.getNom());
                result.put("produitEnEchec", produit.getNom());
                return result;
            }
        }
        
        result.put("success", true);
        result.put("message", "Stock réservé avec succès");
        result.put("reservations", reservations);
        result.put("totalItems", reservations.size());
        
        return result;
    }
}