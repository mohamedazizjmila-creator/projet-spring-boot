package com.example.projet.service;

import com.example.projet.entity.*;
import com.example.projet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PanierService panierService;
    
    @Autowired
    private UserRepository userRepository;
    

    
    // Créer une commande à partir du panier
    @Transactional
    public Order createOrderFromPanier(Long userId, String shippingAddress, String paymentMethod) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        Panier panier = panierService.getPanierByUserId(userId);
        
        if (panier.getItems().isEmpty()) {
            throw new RuntimeException("Le panier est vide");
        }
        
        // Créer la commande
        Order order = new Order(user);
        order.setShippingAddress(shippingAddress);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("PENDING");
        
        // Copier les items du panier vers la commande
        for (PanierItem panierItem : panier.getItems()) {
            OrderItem orderItem = new OrderItem(
                order, 
                panierItem.getProduit(), 
                panierItem.getQuantite()
            );
            order.addItem(orderItem);
        }
        
        // Calculer le total
        order.calculateTotal();
        
        // Sauvegarder la commande
        Order savedOrder = orderRepository.save(order);
        
        // Vider le panier
        panierService.clearPanier(userId);
        
        return savedOrder;
    }
    
    // Récupérer les commandes d'un utilisateur
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }
    
    // Récupérer une commande par son ID
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
    }
    
    // Mettre à jour le statut d'une commande
    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        
        // Mettre à jour les dates selon le statut
        if (status == Order.OrderStatus.SHIPPED) {
            order.setShippedDate(java.time.LocalDateTime.now());
        } else if (status == Order.OrderStatus.DELIVERED) {
            order.setDeliveredDate(java.time.LocalDateTime.now());
        }
        
        return orderRepository.save(order);
    }
    @Autowired
    private OrderItemService orderItemService;

    // Nouvelle méthode pour récupérer les détails d'une commande avec ses items
    public Map<String, Object> getOrderDetails(Long orderId) {
        Order order = getOrderById(orderId);
        List<OrderItem> items = orderItemService.getOrderItemsByOrderId(orderId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("order", order);
        response.put("items", items);
        response.put("totalItems", items.size());
        
        return response;
    }
 // Dans OrderService.java
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
}