package com.example.projet.controller;

import com.example.projet.entity.PanierItem;
import com.example.projet.entity.Produit;
import com.example.projet.service.PanierItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/panier-items")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class PanierItemController {
    
    @Autowired
    private PanierItemService panierItemService;
    
    // ============ ENDPOINTS DE RÉCUPÉRATION ============
    
    // Récupérer un item par ID
    @GetMapping("/{itemId}")
    public ResponseEntity<?> getPanierItem(@PathVariable Long itemId) {
        try {
            PanierItem item = panierItemService.getPanierItemById(itemId);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Récupérer tous les items d'un panier
    @GetMapping("/panier/{panierId}")
    public ResponseEntity<?> getPanierItemsByPanierId(@PathVariable Long panierId) {
        try {
            List<PanierItem> items = panierItemService.getPanierItemsByPanierId(panierId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Récupérer les items du panier d'un utilisateur
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPanierItemsByUserId(@PathVariable Long userId) {
        try {
            List<PanierItem> items = panierItemService.getPanierItemsByUserId(userId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Récupérer un item spécifique par panier et produit
    @GetMapping("/rechercher")
    public ResponseEntity<?> getPanierItem(
            @RequestParam Long panierId,
            @RequestParam Long produitId) {
        try {
            Optional<PanierItem> item = panierItemService.getPanierItem(panierId, produitId);
            
            Map<String, Object> response = new HashMap<>();
            if (item.isPresent()) {
                response.put("found", true);
                response.put("item", item.get());
            } else {
                response.put("found", false);
                response.put("message", "Produit non trouvé dans le panier");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Vérifier si un produit est dans le panier
    @GetMapping("/verifier")
    public ResponseEntity<?> isProductInPanier(
            @RequestParam Long panierId,
            @RequestParam Long produitId) {
        try {
            boolean isInPanier = panierItemService.isProductInPanier(panierId, produitId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("panierId", panierId);
            response.put("produitId", produitId);
            response.put("isInPanier", isInPanier);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Compter les items d'un panier
    @GetMapping("/panier/{panierId}/count")
    public ResponseEntity<?> countPanierItems(@PathVariable Long panierId) {
        try {
            Long count = panierItemService.countPanierItems(panierId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("panierId", panierId);
            response.put("count", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Calculer le total d'un panier
    @GetMapping("/panier/{panierId}/total")
    public ResponseEntity<?> calculatePanierTotal(@PathVariable Long panierId) {
        try {
            Double total = panierItemService.calculatePanierTotal(panierId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("panierId", panierId);
            response.put("total", total);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Récupérer les détails complets d'un panier
    @GetMapping("/panier/{panierId}/details")
    public ResponseEntity<?> getPanierDetails(@PathVariable Long panierId) {
        try {
            PanierItemService.PanierDetails details = panierItemService.getPanierDetails(panierId);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // ============ ENDPOINTS D'ACTION ============
    
    // Ajouter un produit au panier
    @PostMapping("/ajouter")
    public ResponseEntity<?> addToPanier(
            @RequestParam Long panierId,
            @RequestParam Long produitId,
            @RequestParam(defaultValue = "1") Integer quantite) {
        try {
            PanierItem item = panierItemService.addToPanier(panierId, produitId, quantite);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Produit ajouté au panier");
            response.put("item", item);
            
            // Informations supplémentaires sur le panier
            response.put("panierId", panierId);
            response.put("produitId", produitId);
            response.put("quantite", quantite);
            
            // Statistiques du panier mis à jour
            Long count = panierItemService.countPanierItems(panierId);
            Double total = panierItemService.calculatePanierTotal(panierId);
            response.put("panierCount", count);
            response.put("panierTotal", total);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Mettre à jour la quantité d'un item
    @PutMapping("/{itemId}/quantite")
    public ResponseEntity<?> updatePanierItemQuantite(
            @PathVariable Long itemId,
            @RequestParam Integer quantite) {
        try {
            PanierItem updatedItem = panierItemService.updatePanierItemQuantite(itemId, quantite);
            
            Map<String, Object> response = new HashMap<>();
            
            if (updatedItem == null) {
                // Item supprimé car quantité <= 0
                response.put("success", true);
                response.put("message", "Item supprimé du panier (quantité <= 0)");
                response.put("itemDeleted", true);
                response.put("itemId", itemId);
            } else {
                // Item mis à jour
                response.put("success", true);
                response.put("message", "Quantité mise à jour");
                response.put("item", updatedItem);
                response.put("itemUpdated", true);
                
                // Informations sur le panier parent
                if (updatedItem.getPanier() != null) {
                    Long panierId = updatedItem.getPanier().getId();
                    Long count = panierItemService.countPanierItems(panierId);
                    Double total = panierItemService.calculatePanierTotal(panierId);
                    response.put("panierCount", count);
                    response.put("panierTotal", total);
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Supprimer un item du panier par panier et produit
    @DeleteMapping("/retirer")
    public ResponseEntity<?> removeFromPanier(
            @RequestParam Long panierId,
            @RequestParam Long produitId) {
        try {
            panierItemService.removeFromPanier(panierId, produitId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Produit retiré du panier");
            response.put("panierId", panierId);
            response.put("produitId", produitId);
            
            // Statistiques du panier mis à jour
            Long count = panierItemService.countPanierItems(panierId);
            Double total = panierItemService.calculatePanierTotal(panierId);
            response.put("panierCount", count);
            response.put("panierTotal", total);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Supprimer un item par ID
    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> deletePanierItem(@PathVariable Long itemId) {
        try {
            // Récupérer l'item avant suppression pour avoir le panierId
            PanierItem item = panierItemService.getPanierItemById(itemId);
            Long panierId = item.getPanier().getId();
            
            panierItemService.deletePanierItem(itemId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Item supprimé du panier");
            response.put("itemId", itemId);
            response.put("panierId", panierId);
            
            // Statistiques du panier mis à jour
            Long count = panierItemService.countPanierItems(panierId);
            Double total = panierItemService.calculatePanierTotal(panierId);
            response.put("panierCount", count);
            response.put("panierTotal", total);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Vider le panier
    @DeleteMapping("/panier/{panierId}/vider")
    public ResponseEntity<?> clearPanier(@PathVariable Long panierId) {
        try {
            panierItemService.clearPanier(panierId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Panier vidé avec succès");
            response.put("panierId", panierId);
            response.put("panierCount", 0);
            response.put("panierTotal", 0.0);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // Vider le panier d'un utilisateur
    @DeleteMapping("/user/{userId}/vider")
    public ResponseEntity<?> clearUserPanier(@PathVariable Long userId) {
        try {
            panierItemService.clearUserPanier(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Panier de l'utilisateur vidé avec succès");
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // ============ ENDPOINTS DE STATISTIQUES ============
    
    // Récupérer les produits les plus ajoutés aux paniers
    @GetMapping("/statistiques/produits-populaires")
    public ResponseEntity<?> getMostAddedProducts() {
        try {
            List<Object[]> topProducts = panierItemService.getMostAddedProducts();
            
            // Transformer les résultats en format plus lisible
            List<Map<String, Object>> formattedProducts = topProducts.stream()
                .map(row -> {
                    Map<String, Object> product = new HashMap<>();
                    if (row.length > 0 && row[0] instanceof Object[]) {
                        // Si c'est un tableau d'objets
                        Object[] productArray = (Object[]) row[0];
                        if (productArray.length > 0) product.put("produitId", productArray[0]);
                        if (productArray.length > 1) product.put("nom", productArray[1]);
                        if (productArray.length > 2) product.put("count", productArray[2]);
                    } else {
                        // Format simple
                        for (int i = 0; i < row.length; i++) {
                            if (i == 0) product.put("produitId", row[i]);
                            if (i == 1) product.put("nom", row[i]);
                            if (i == 2) product.put("count", row[i]);
                        }
                    }
                    return product;
                })
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("topProducts", formattedProducts);
            response.put("totalProducts", formattedProducts.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // ============ ENDPOINTS UTILITAIRES ============
    
    // Vérifier la disponibilité d'un produit dans le panier
    @GetMapping("/disponibilite")
    public ResponseEntity<?> checkProductAvailability(
            @RequestParam Long panierId,
            @RequestParam Long produitId) {
        try {
            // Vérifier si le produit est dans le panier
            Optional<PanierItem> itemOptional = panierItemService.getPanierItem(panierId, produitId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("panierId", panierId);
            response.put("produitId", produitId);
            
            if (itemOptional.isPresent()) {
                PanierItem item = itemOptional.get();
                Produit produit = item.getProduit();
                Integer quantiteDansPanier = item.getQuantite();
                Integer stockDisponible = produit.getQuantite();
                
                response.put("inPanier", true);
                response.put("quantiteDansPanier", quantiteDansPanier);
                response.put("stockDisponible", stockDisponible);
                response.put("suffisant", stockDisponible >= quantiteDansPanier);
                
                if (stockDisponible < quantiteDansPanier) {
                    response.put("message", "Stock insuffisant dans le panier");
                } else {
                    response.put("message", "Stock suffisant");
                }
            } else {
                response.put("inPanier", false);
                response.put("message", "Produit non présent dans le panier");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    // ============ MÉTHODE UTILITAIRE ============
    
    private ResponseEntity<?> createErrorResponse(Exception e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", e.getMessage());
        response.put("timestamp", System.currentTimeMillis());
        
        // Log supplémentaire pour le débogage
        e.printStackTrace();
        
        return ResponseEntity.badRequest().body(response);
    }
}