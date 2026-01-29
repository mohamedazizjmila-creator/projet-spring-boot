package com.example.projet.controller;

import com.example.projet.entity.OrderItem;
import com.example.projet.service.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order-items")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class OrderItemController {
    
    @Autowired
    private OrderItemService orderItemService;
    
    // Récupérer les items d'une commande
    @GetMapping("/commande/{orderId}")
    public ResponseEntity<?> getOrderItems(@PathVariable Long orderId) {
        try {
            List<OrderItem> items = orderItemService.getOrderItemsByOrderId(orderId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Récupérer un item spécifique
    @GetMapping("/{itemId}")
    public ResponseEntity<?> getOrderItem(@PathVariable Long itemId) {
        try {
            OrderItem item = orderItemService.getOrderItemById(itemId);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Ajouter un item à une commande
    @PostMapping("/ajouter")
    public ResponseEntity<?> addOrderItem(
            @RequestParam Long orderId,
            @RequestParam Long produitId,
            @RequestParam Integer quantite) {
        try {
            OrderItem item = orderItemService.addItemToOrder(orderId, produitId, quantite);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Mettre à jour la quantité d'un item
    @PutMapping("/{itemId}/update-quantite")
    public ResponseEntity<?> updateQuantite(
            @PathVariable Long itemId,
            @RequestParam Integer quantite) {
        try {
            OrderItem item = orderItemService.updateOrderItemQuantity(itemId, quantite);
            if (item == null) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Item supprimé (quantité <= 0)");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Supprimer un item
    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> removeOrderItem(@PathVariable Long itemId) {
        try {
            orderItemService.removeOrderItem(itemId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Item supprimé avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Supprimer tous les items d'une commande
    @DeleteMapping("/commande/{orderId}/vider")
    public ResponseEntity<?> clearOrderItems(@PathVariable Long orderId) {
        try {
            orderItemService.deleteOrderItems(orderId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Tous les items de la commande ont été supprimés");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Récupérer les statistiques (pour admin)
    @GetMapping("/statistiques/top-produits")
    public ResponseEntity<?> getTopSellingProducts(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Object[]> topProducts = orderItemService.getTopSellingProducts(limit);
            return ResponseEntity.ok(topProducts);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Récupérer le total des ventes par produit
    @GetMapping("/statistiques/produit/{produitId}/total")
    public ResponseEntity<?> getTotalSalesByProduct(@PathVariable Long produitId) {
        try {
            Double total = orderItemService.getTotalSalesByProduct(produitId);
            return ResponseEntity.ok(total);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}