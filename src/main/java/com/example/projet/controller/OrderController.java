package com.example.projet.controller;

import com.example.projet.entity.Order;
import com.example.projet.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    // Créer une commande
    @PostMapping("/creer")
    public ResponseEntity<?> creerCommande(
            @RequestParam Long userId,
            @RequestParam String shippingAddress,
            @RequestParam String paymentMethod) {
        try {
            Order order = orderService.createOrderFromPanier(userId, shippingAddress, paymentMethod);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Récupérer les commandes de l'utilisateur
    @GetMapping("/mes-commandes")
    public ResponseEntity<?> getMesCommandes(@RequestParam Long userId) {
        try {
            List<Order> orders = orderService.getUserOrders(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Récupérer une commande spécifique
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getCommande(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Récupérer les détails d'une commande (avec items)
    @GetMapping("/{orderId}/details")
    public ResponseEntity<?> getCommandeDetails(@PathVariable Long orderId) {
        try {
            Map<String, Object> details = orderService.getOrderDetails(orderId);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Annuler une commande
    @PutMapping("/{orderId}/annuler")
    public ResponseEntity<?> annulerCommande(@PathVariable Long orderId) {
        try {
            Order order = orderService.updateOrderStatus(orderId, Order.OrderStatus.CANCELLED);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Supprimer une commande (seulement si annulée)
  
    
    // Récupérer toutes les commandes (pour admin)
    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}