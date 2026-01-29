package com.example.projet.repository;

import com.example.projet.entity.Panier;
import com.example.projet.entity.PanierItem;
import com.example.projet.entity.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PanierItemRepository extends JpaRepository<PanierItem, Long> {
    
    // Trouver un item par panier et produit
    Optional<PanierItem> findByPanierAndProduit(Panier panier, Produit produit);
    
    // Trouver tous les items d'un panier
    List<PanierItem> findByPanier(Panier panier);
    
    // Trouver tous les items d'un panier par ID
    List<PanierItem> findByPanierId(Long panierId);
    
    // Vérifier si un produit existe dans un panier
    boolean existsByPanierAndProduit(Panier panier, Produit produit);
    
    // Supprimer par panier et produit
    @Transactional
    @Modifying
    void deleteByPanierAndProduit(Panier panier, Produit produit);
    
    // Supprimer tous les items d'un panier
    @Transactional
    @Modifying
    void deleteByPanier(Panier panier);
    
    // Supprimer par ID de panier
    @Transactional
    @Modifying
    @Query("DELETE FROM PanierItem pi WHERE pi.panier.id = :panierId")
    void deleteByPanierId(@Param("panierId") Long panierId);
    
    // Compter le nombre d'items dans un panier
    @Query("SELECT COUNT(pi) FROM PanierItem pi WHERE pi.panier.id = :panierId")
    Long countByPanierId(@Param("panierId") Long panierId);
    
    // Calculer le total d'un panier
    @Query("SELECT SUM(pi.quantite * p.prix) FROM PanierItem pi JOIN pi.produit p WHERE pi.panier.id = :panierId")
    Double calculatePanierTotal(@Param("panierId") Long panierId);
    
    // Trouver les produits les plus ajoutés aux paniers
    @Query("SELECT pi.produit, SUM(pi.quantite) as totalQuantite " +
           "FROM PanierItem pi " +
           "GROUP BY pi.produit " +
           "ORDER BY totalQuantite DESC")
    List<Object[]> findMostAddedProducts();
    
    // Récupérer le panier d'un utilisateur avec ses items
    @Query("SELECT pi FROM PanierItem pi JOIN FETCH pi.produit WHERE pi.panier.user.id = :userId")
    List<PanierItem> findPanierItemsByUserId(@Param("userId") Long userId);
    
    // Mettre à jour la quantité d'un item
    @Transactional
    @Modifying
    @Query("UPDATE PanierItem pi SET pi.quantite = :quantite WHERE pi.id = :itemId")
    void updateQuantite(@Param("itemId") Long itemId, @Param("quantite") Integer quantite);
}