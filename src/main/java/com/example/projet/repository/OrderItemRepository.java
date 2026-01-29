package com.example.projet.repository;

import com.example.projet.entity.Order;
import com.example.projet.entity.OrderItem;
import com.example.projet.entity.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(Order order);
    List<OrderItem> findByOrderId(Long orderId);
    List<OrderItem> findByProduit(Produit produit);
    void deleteByOrder(Order order);
    
    // Produits les plus vendus
    @Query("SELECT oi.produit, SUM(oi.quantite) as totalVendu " +
           "FROM OrderItem oi " +
           "GROUP BY oi.produit " +
           "ORDER BY totalVendu DESC")
    List<Object[]> findTopSellingProducts(@Param("limit") int limit);
    
    // Total des ventes par produit
    @Query("SELECT SUM(oi.subtotal) FROM OrderItem oi WHERE oi.produit.id = :produitId")
    Double findTotalSalesByProduct(@Param("produitId") Long produitId);
    
    // Ventes par période
    @Query("SELECT oi.produit, SUM(oi.quantite), MONTH(o.orderDate), YEAR(o.orderDate) " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE YEAR(o.orderDate) = :year " +
           "GROUP BY oi.produit, MONTH(o.orderDate), YEAR(o.orderDate) " +
           "ORDER BY YEAR(o.orderDate), MONTH(o.orderDate)")
    List<Object[]> findSalesByMonth(@Param("year") int year);
}