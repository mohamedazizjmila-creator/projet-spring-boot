package com.example.projet.service;

import com.example.projet.entity.Order;
import com.example.projet.entity.OrderItem;
import com.example.projet.entity.Produit;
import com.example.projet.repository.OrderItemRepository;
import com.example.projet.repository.OrderRepository;
import com.example.projet.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class OrderItemService {
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProduitRepository produitRepository;
    
    // Récupérer tous les items d'une commande
    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }
    
    // Récupérer un item spécifique
    public OrderItem getOrderItemById(Long itemId) {
        return orderItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item de commande non trouvé"));
    }
    
    // Ajouter un item à une commande existante
    @Transactional
    public OrderItem addItemToOrder(Long orderId, Long produitId, Integer quantite) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        
        // Vérifier si le produit est déjà dans la commande
        List<OrderItem> existingItems = orderItemRepository.findByOrder(order);
        for (OrderItem item : existingItems) {
            if (item.getProduit().getId().equals(produitId)) {
                // Mettre à jour la quantité
                item.setQuantite(item.getQuantite() + quantite);
                orderItemRepository.save(item);
                order.calculateTotal();
                orderRepository.save(order);
                return item;
            }
        }
        
        // Ajouter un nouvel item
        OrderItem newItem = new OrderItem(order, produit, quantite);
        orderItemRepository.save(newItem);
        
        // Recalculer le total de la commande
        order.calculateTotal();
        orderRepository.save(order);
        
        return newItem;
    }
    
    // Mettre à jour la quantité d'un item
    @Transactional
    public OrderItem updateOrderItemQuantity(Long itemId, Integer quantite) {
        OrderItem item = getOrderItemById(itemId);
        
        if (quantite <= 0) {
            // Supprimer l'item si quantité <= 0
            Order order = item.getOrder();
            orderItemRepository.delete(item);
            order.calculateTotal();
            orderRepository.save(order);
            return null;
        }
        
        item.setQuantite(quantite);
        OrderItem updatedItem = orderItemRepository.save(item);
        
        // Recalculer le total de la commande
        Order order = item.getOrder();
        order.calculateTotal();
        orderRepository.save(order);
        
        return updatedItem;
    }
    
    // Supprimer un item de commande
    @Transactional
    public void removeOrderItem(Long itemId) {
        OrderItem item = getOrderItemById(itemId);
        Order order = item.getOrder();
        
        orderItemRepository.delete(item);
        
        // Recalculer le total de la commande
        order.calculateTotal();
        orderRepository.save(order);
    }
    
    // Supprimer tous les items d'une commande
    @Transactional
    public void deleteOrderItems(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        
        if (!items.isEmpty()) {
            orderItemRepository.deleteAll(items);
            
            // Recalculer le total de la commande (0)
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
            order.calculateTotal();
            orderRepository.save(order);
        }
    }
    
    // Récupérer les statistiques des produits vendus
    public List<Object[]> getTopSellingProducts(int limit) {
        return orderItemRepository.findTopSellingProducts(limit);
    }
    
    // Récupérer le total des ventes par produit
    public Double getTotalSalesByProduct(Long produitId) {
        return orderItemRepository.findTotalSalesByProduct(produitId);
    }
}