package com.example.projet.repository;

import com.example.projet.entity.Order;
import com.example.projet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(Order.OrderStatus status);
    Optional<Order> findByOrderNumber(String orderNumber);
}