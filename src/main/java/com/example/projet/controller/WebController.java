package com.example.projet.controller;

import com.example.projet.entity.*;
import com.example.projet.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@SessionAttributes("currentUser")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class WebController {
    
    @Autowired
    private ProduitService produitService;
    
    @Autowired
    private CategorieService categorieService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    // ==================== NOUVEAUX SERVICES ====================
    
    @Autowired
    private PanierService panierService;
    
    @Autowired
    private PanierItemService panierItemService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderItemService orderItemService;
    
    @Autowired
    private FavoriService favoriService;
    
    // ==================== PAGES D'AUTHENTIFICATION ====================
    
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
    
    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
    
    // ==================== PAGE D'ACCUEIL ====================
    
    @GetMapping("/")
    public String homePage(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        long totalProduits = produitService.count();
        List<Categorie> categories = categorieService.getAllCategories();
        long totalCategories = categories.size();
        List<Produit> produitsEnStock = produitService.findProduitsEnStock();
        long stockCount = produitsEnStock.size();
        
        List<Produit> tousProduits = produitService.findAll();
        long produitsFaibleStock = tousProduits.stream()
            .filter(p -> p.getQuantite() <= 5)
            .count();
        
        List<Produit> derniersProduits = produitService.findAll();
        if (derniersProduits.size() > 5) {
            derniersProduits = derniersProduits.subList(0, Math.min(5, derniersProduits.size()));
        }
        
        model.addAttribute("totalProduits", totalProduits);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("produitsEnStock", stockCount);
        model.addAttribute("produitsFaibleStock", produitsFaibleStock);
        model.addAttribute("produits", derniersProduits);
        model.addAttribute("categories", categories);
        model.addAttribute("currentUser", currentUser);
        
        return "index";
    }
    
    // ==================== GESTION DU PANIER ====================
    
    @GetMapping("/panier")
    public String viewPanier(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            // Récupérer le panier
            Panier panier = panierService.getPanierByUserId(currentUser.getId());
            
            // Récupérer les items du panier
            List<PanierItem> panierItems = panierService.getPanierItems(currentUser.getId());
            
            // Calculer le total
            double total = 0;
            for (PanierItem item : panierItems) {
                total += item.getProduit().getPrix() * item.getQuantite();
            }
            
            // Calculer les taxes (TVA 20%)
            double tva = total * 0.20;
            double totalTTC = total + tva;
            
            model.addAttribute("panier", panier);
            model.addAttribute("panierItems", panierItems);
            model.addAttribute("total", total);
            model.addAttribute("tva", tva);
            model.addAttribute("totalTTC", totalTTC);
            model.addAttribute("currentUser", currentUser);
            
            return "panier/view";
            
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du chargement du panier: " + e.getMessage());
            return "panier/view";
        }
    }
    
    @PostMapping("/panier/add/{produitId}")
    public String addToPanier(@PathVariable Long produitId,
                             @RequestParam(defaultValue = "1") Integer quantite,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            // Vérifier si le produit existe
            Optional<Produit> produitOpt = produitService.findById(produitId);
            if (produitOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Produit non trouvé");
                return "redirect:/produits";
            }
            
            Produit produit = produitOpt.get();
            
            // Vérifier le stock
            if (produit.getQuantite() < quantite) {
                redirectAttributes.addFlashAttribute("error", "Stock insuffisant. Disponible: " + produit.getQuantite());
                return "redirect:/produits";
            }
            
            // Ajouter au panier
            panierService.addToPanier(currentUser.getId(), produitId, quantite);
            
            redirectAttributes.addFlashAttribute("success", "Produit ajouté au panier");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        
        return "redirect:/produits";
    }
    
    @PostMapping("/panier/update/{produitId}")
    public String updatePanierItem(@PathVariable Long produitId,
                                  @RequestParam Integer quantite,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            panierService.updatePanierItem(currentUser.getId(), produitId, quantite);
            redirectAttributes.addFlashAttribute("success", "Quantité mise à jour");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        
        return "redirect:/panier";
    }
    
    @GetMapping("/panier/remove/{produitId}")
    public String removeFromPanier(@PathVariable Long produitId,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            panierService.removeFromPanier(currentUser.getId(), produitId);
            redirectAttributes.addFlashAttribute("success", "Produit retiré du panier");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        
        return "redirect:/panier";
    }
    
    @GetMapping("/panier/clear")
    public String clearPanier(HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            panierService.clearPanier(currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Panier vidé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        
        return "redirect:/panier";
    }
    
    // ==================== GESTION DES COMMANDES ====================
    
    @PostMapping("/commande/passer")
    public String passerCommande(@RequestParam String shippingAddress,
                                @RequestParam String paymentMethod,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            // Vérifier si le panier n'est pas vide
            Panier panier = panierService.getPanierByUserId(currentUser.getId());
            if (panier.getItems().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Votre panier est vide");
                return "redirect:/panier";
            }
            
            // Vérifier la disponibilité des produits
            boolean stockOK = true;
            StringBuilder stockErrors = new StringBuilder();
            for (PanierItem item : panier.getItems()) {
                Produit produit = item.getProduit();
                if (produit.getQuantite() < item.getQuantite()) {
                    stockOK = false;
                    stockErrors.append("Stock insuffisant pour: ").append(produit.getNom())
                              .append(" (demandé: ").append(item.getQuantite())
                              .append(", disponible: ").append(produit.getQuantite()).append(")\n");
                }
            }
            
            if (!stockOK) {
                redirectAttributes.addFlashAttribute("error", stockErrors.toString());
                return "redirect:/panier";
            }
            
            // Créer la commande
            Order order = orderService.createOrderFromPanier(currentUser.getId(), shippingAddress, paymentMethod);
            
            redirectAttributes.addFlashAttribute("success", "Commande passée avec succès! Numéro: " + order.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la commande: " + e.getMessage());
        }
        
        return "redirect:/commandes";
    }
    
    @GetMapping("/commandes")
    public String listCommandes(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            List<Order> commandes;
            if (currentUser.getRole() != null && currentUser.getRole().equals("ADMIN")) {
                // Admin voit toutes les commandes
                commandes = orderService.getAllOrders(); // Vous devrez ajouter cette méthode
            } else {
                // Utilisateur normal voit ses propres commandes
                commandes = orderService.getUserOrders(currentUser.getId());
            }
            
            model.addAttribute("commandes", commandes);
            model.addAttribute("currentUser", currentUser);
            
            return "commande/list";
            
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du chargement des commandes: " + e.getMessage());
            return "commande/list";
        }
    }
    
    @GetMapping("/commandes/{id}")
    public String viewCommande(@PathVariable Long id, Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            Order order = orderService.getOrderById(id);
            
            // Vérifier que l'utilisateur peut voir cette commande
            if (!order.getUser().getId().equals(currentUser.getId()) && 
                !(currentUser.getRole() != null && currentUser.getRole().equals("ADMIN"))) {
                model.addAttribute("error", "Accès non autorisé à cette commande");
                return "redirect:/commandes";
            }
            
            List<OrderItem> items = orderItemService.getOrderItemsByOrderId(id);
            
            // Calculer le total
            double total = 0;
            for (OrderItem item : items) {
                total += item.getProduit().getPrix() * item.getQuantite();
            }
            
            model.addAttribute("order", order);
            model.addAttribute("orderItems", items);
            model.addAttribute("total", total);
            model.addAttribute("currentUser", currentUser);
            
            return "commande/detail";
            
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du chargement de la commande: " + e.getMessage());
            return "redirect:/commandes";
        }
    }
    
    @PostMapping("/commandes/{id}/status")
    public String updateStatus(@PathVariable Long id,
                              @RequestParam String status,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !currentUser.getRole().equals("ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Accès réservé aux administrateurs");
            return "redirect:/login";
        }
        
        try {
            Order order = orderService.getOrderById(id);
            
            // Convertir le statut string en enum
            Order.OrderStatus orderStatus;
            try {
                orderStatus = Order.OrderStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Statut invalide: " + status);
                return "redirect:/commandes/" + id;
            }
            
            orderService.updateOrderStatus(id, orderStatus);
            
            redirectAttributes.addFlashAttribute("success", "Statut de la commande mis à jour");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        
        return "redirect:/commandes/" + id;
    }
    
    // ==================== GESTION DES FAVORIS ====================
    
    @GetMapping("/favoris")
    public String viewFavoris(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            List<Favori> favorisList = favoriService.getUserFavoris(currentUser.getId());
            List<Produit> produitsFavoris = new ArrayList<>();
            
            for (Favori favori : favorisList) {
                produitsFavoris.add(favori.getProduit());
            }
            
            model.addAttribute("produitsFavoris", produitsFavoris);
            model.addAttribute("currentUser", currentUser);
            
            return "favoris/list";
            
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du chargement des favoris: " + e.getMessage());
            return "favoris/list";
        }
    }
    
    @PostMapping("/favoris/toggle/{produitId}")
    public String toggleFavoris(@PathVariable Long produitId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            // Vérifier si le produit existe
            Optional<Produit> produitOpt = produitService.findById(produitId);
            if (produitOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Produit non trouvé");
                return "redirect:/produits";
            }
            
            // Vérifier si le produit est déjà dans les favoris
            boolean isInFavoris = favoriService.isProductInFavoris(currentUser.getId(), produitId);
            
            if (isInFavoris) {
                // Retirer des favoris
                favoriService.removeFromFavoris(currentUser.getId(), produitId);
                redirectAttributes.addFlashAttribute("success", "Retiré des favoris");
            } else {
                // Ajouter aux favoris
                favoriService.addToFavoris(currentUser.getId(), produitId);
                redirectAttributes.addFlashAttribute("success", "Ajouté aux favoris");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        
        // Rediriger vers la page précédente ou la liste des produits
        String referer = (String) session.getAttribute("referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        
        return "redirect:/favoris";
    }
    
    @GetMapping("/favoris/remove/{produitId}")
    public String removeFavoris(@PathVariable Long produitId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            favoriService.removeFromFavoris(currentUser.getId(), produitId);
            redirectAttributes.addFlashAttribute("success", "Retiré des favoris");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        
        return "redirect:/favoris";
    }
    
    @GetMapping("/favoris/clear")
    public String clearFavoris(HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            favoriService.clearFavoris(currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Tous les favoris ont été supprimés");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        
        return "redirect:/favoris";
    }
    
    // ==================== PAGE ADMIN UTILISATEURS ====================
    
    @GetMapping("/admin/users")
    public String adminUsersPage(Model model, HttpSession session) {
        // Récupérer l'utilisateur depuis la session
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // Vérifier si l'utilisateur est admin (vérification simplifiée)
        if (currentUser.getRole() == null || !currentUser.getRole().equals("ADMIN")) {
            model.addAttribute("error", "Accès réservé aux administrateurs");
            return "redirect:/";
        }
        
        try {
            // Récupérer tous les utilisateurs avec un try-catch
            List<User> users = userService.findAll();
            
            // Ne pas renvoyer les mots de passe pour la sécurité
            users.forEach(user -> user.setPassword(null));
            
            // Compter les statistiques avec gestion d'erreurs
            long totalUsers = 0;
            long adminCount = 0;
            long userCount = 0;
            
            try {
                totalUsers = userService.count();
            } catch (Exception e) {
                model.addAttribute("error", "Problème de données: " + e.getMessage());
                // Utiliser la taille de la liste comme fallback
                totalUsers = users.size();
            }
            
            try {
                adminCount = userService.countByRole("ADMIN");
            } catch (Exception e) {
                adminCount = users.stream()
                    .filter(u -> u.getRole() != null && u.getRole().equals("ADMIN"))
                    .count();
            }
            
            try {
                userCount = userService.countByRole("USER");
            } catch (Exception e) {
                userCount = users.stream()
                    .filter(u -> u.getRole() != null && u.getRole().equals("USER"))
                    .count();
            }
            
            // Si totalUsers est 0 mais qu'on a des utilisateurs, utiliser la taille de la liste
            if (totalUsers == 0 && !users.isEmpty()) {
                totalUsers = users.size();
            }
            
            // Ajouter les données au modèle
            model.addAttribute("users", users);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("adminCount", adminCount);
            model.addAttribute("userCount", userCount);
            
        } catch (Exception e) {
            // En cas d'erreur, renvoyer des données vides avec un message d'erreur
            model.addAttribute("error", "Erreur lors du chargement des utilisateurs: " + e.getMessage());
            model.addAttribute("users", new ArrayList<User>());
            model.addAttribute("totalUsers", 0);
            model.addAttribute("adminCount", 0);
            model.addAttribute("userCount", 0);
        }
        
        return "admin/users";
    }
    // ==================== PROFIL UTILISATEUR ====================
    
    @GetMapping("/profile")
    public String profilePage(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", currentUser);
        return "user/profile";
    }
    
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute User userDetails, 
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            currentUser.setEmail(userDetails.getEmail());
            userService.save(currentUser);
            session.setAttribute("currentUser", currentUser);
            
            redirectAttributes.addFlashAttribute("success", "Profil mis à jour avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }
        
        return "redirect:/profile";
    }
    
    // ==================== GESTION DES PRODUITS ====================
    
    @GetMapping("/produits")
    public String listProduits(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        List<Produit> produits = produitService.findAll();
        List<Categorie> categories = categorieService.getAllCategories();
        
        // Récupérer les favoris de l'utilisateur
        List<Long> favorisIds = new ArrayList<>();
        if (currentUser != null) {
            List<Favori> favorisList = favoriService.getUserFavoris(currentUser.getId());
            for (Favori favori : favorisList) {
                favorisIds.add(favori.getProduit().getId());
            }
        }
        
        model.addAttribute("produits", produits);
        model.addAttribute("categories", categories);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("favorisIds", favorisIds);
        return "produit/list";
    }
    
    @GetMapping("/produits/add")
    public String addProduitPage(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        List<Categorie> categories = categorieService.getAllCategories();
        model.addAttribute("produit", new Produit());
        model.addAttribute("categories", categories);
        model.addAttribute("currentUser", currentUser);
        return "produit/add";
    }
    
    @PostMapping("/produits/add")
    public String addProduit(@ModelAttribute Produit produit, 
                            @RequestParam(value = "categorieId", required = false) Long categorieId,
                            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            if (categorieId != null) {
                Optional<Categorie> categorie = categorieService.getCategorieById(categorieId);
                categorie.ifPresent(produit::setCategorie);
            }
            
            if (imageFile != null && !imageFile.isEmpty()) {
                if (!fileStorageService.isImageFile(imageFile)) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Veuillez uploader une image valide (JPG, PNG, GIF)");
                    return "redirect:/produits/add";
                }
                
                if (!fileStorageService.isFileSizeValid(imageFile)) {
                    redirectAttributes.addFlashAttribute("error", 
                        "L'image est trop grande. Maximum 5MB");
                    return "redirect:/produits/add";
                }
                
                String fileName = fileStorageService.saveImage(imageFile);
                produit.setImageName(fileName);
                produit.setImageUrl(fileStorageService.getImageUrl(fileName));
            }
            
            produitService.save(produit);
            redirectAttributes.addFlashAttribute("success", "Produit ajouté avec succès !");
            return "redirect:/produits";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de l'ajout: " + e.getMessage());
            return "redirect:/produits/add";
        }
    }
    
    @GetMapping("/produits/edit/{id}")
    public String editProduitPage(@PathVariable Long id, Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        Optional<Produit> produit = produitService.findById(id);
        List<Categorie> categories = categorieService.getAllCategories();
        
        if (produit.isPresent()) {
            model.addAttribute("produit", produit.get());
            model.addAttribute("categories", categories);
            model.addAttribute("currentUser", currentUser);
            return "produit/edit";
        } else {
            return "redirect:/produits";
        }
    }

    @PostMapping("/produits/update")
    public String updateProduit(@ModelAttribute Produit produit, 
                               @RequestParam(value = "categorieId", required = false) Long categorieId,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               @RequestParam(value = "removeImage", defaultValue = "false") boolean removeImage,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            Optional<Produit> existingProduitOpt = produitService.findById(produit.getId());
            
            if (existingProduitOpt.isPresent()) {
                Produit existingProduit = existingProduitOpt.get();
                
                produit.setImageName(existingProduit.getImageName());
                produit.setImageUrl(existingProduit.getImageUrl());
                
                if (removeImage) {
                    if (existingProduit.getImageName() != null) {
                        try {
                            fileStorageService.deleteImage(existingProduit.getImageName());
                        } catch (IOException e) {
                            System.err.println("Erreur suppression image: " + e.getMessage());
                        }
                    }
                    produit.setImageName(null);
                    produit.setImageUrl(null);
                }
                
                if (imageFile != null && !imageFile.isEmpty()) {
                    if (!fileStorageService.isImageFile(imageFile)) {
                        redirectAttributes.addFlashAttribute("error", 
                            "Veuillez uploader une image valide (JPG, PNG, GIF)");
                        return "redirect:/produits/edit/" + produit.getId();
                    }
                    
                    if (!fileStorageService.isFileSizeValid(imageFile)) {
                        redirectAttributes.addFlashAttribute("error", 
                            "L'image est trop grande. Maximum 5MB");
                        return "redirect:/produits/edit/" + produit.getId();
                    }
                    
                    if (existingProduit.getImageName() != null && !removeImage) {
                        try {
                            fileStorageService.deleteImage(existingProduit.getImageName());
                        } catch (IOException e) {
                            System.err.println("Erreur suppression ancienne image: " + e.getMessage());
                        }
                    }
                    
                    String fileName = fileStorageService.saveImage(imageFile);
                    produit.setImageName(fileName);
                    produit.setImageUrl(fileStorageService.getImageUrl(fileName));
                }
            }
            
            if (categorieId != null) {
                Optional<Categorie> categorie = categorieService.getCategorieById(categorieId);
                categorie.ifPresent(produit::setCategorie);
            }
            
            produitService.save(produit);
            redirectAttributes.addFlashAttribute("success", "Produit mis à jour avec succès !");
            return "redirect:/produits";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de la mise à jour: " + e.getMessage());
            return "redirect:/produits/edit/" + produit.getId();
        }
    }
    
    @GetMapping("/produits/delete/{id}")
    public String deleteProduit(@PathVariable Long id, 
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            Optional<Produit> produitOpt = produitService.findById(id);
            if (produitOpt.isPresent()) {
                Produit produit = produitOpt.get();
                if (produit.getImageName() != null) {
                    try {
                        fileStorageService.deleteImage(produit.getImageName());
                    } catch (IOException e) {
                        System.err.println("Erreur lors de la suppression de l'image: " + e.getMessage());
                    }
                }
            }
            
            produitService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Produit supprimé avec succès");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de la suppression: " + e.getMessage());
        }
        
        return "redirect:/produits";
    }
    
    @GetMapping("/produits/{id}/remove-image")
    public String removeImage(@PathVariable Long id, 
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            Optional<Produit> produitOpt = produitService.findById(id);
            if (produitOpt.isPresent()) {
                Produit produit = produitOpt.get();
                
                if (produit.getImageName() != null) {
                    fileStorageService.deleteImage(produit.getImageName());
                }
                
                produit.setImageName(null);
                produit.setImageUrl(null);
                produitService.save(produit);
                
                redirectAttributes.addFlashAttribute("success", "Image supprimée avec succès");
            } else {
                redirectAttributes.addFlashAttribute("error", "Produit non trouvé");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de la suppression de l'image: " + e.getMessage());
        }
        
        return "redirect:/produits/edit/" + id;
    }
    
    // ==================== GESTION DES CATÉGORIES ====================
    
    @GetMapping("/categories")
    public String listCategories(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        List<Categorie> categories = categorieService.getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("currentUser", currentUser);
        return "categorie/list";
    }
    
    @GetMapping("/categories/add")
    public String addCategoriePage(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("categorie", new Categorie());
        model.addAttribute("currentUser", currentUser);
        return "categorie/add";
    }
    
    @PostMapping("/categories/add")
    public String addCategorie(@ModelAttribute Categorie categorie, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        categorieService.saveCategorie(categorie);
        return "redirect:/categories";
    }
    
    @GetMapping("/categories/edit/{id}")
    public String editCategoriePage(@PathVariable Long id, Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        Optional<Categorie> categorie = categorieService.getCategorieById(id);
        if (categorie.isPresent()) {
            model.addAttribute("categorie", categorie.get());
            model.addAttribute("currentUser", currentUser);
            return "categorie/edit";
        } else {
            return "redirect:/categories";
        }
    }
    
    @PostMapping("/categories/update")
    public String updateCategorie(@ModelAttribute Categorie categorie, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        categorieService.saveCategorie(categorie);
        return "redirect:/categories";
    }
    
    @GetMapping("/categories/delete/{id}")
    public String deleteCategorie(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        categorieService.deleteCategorie(id);
        return "redirect:/categories";
    }
    
    // ==================== RECHERCHE ====================
    
    @GetMapping("/produits/search")
    public String searchProduits(@RequestParam("keyword") String keyword, 
                                Model model, 
                                HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        List<Produit> produits = produitService.findByNomContaining(keyword);
        model.addAttribute("produits", produits);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentUser", currentUser);
        return "produit/list";
    }
    
    @GetMapping("/categories/search")
    public String searchCategories(@RequestParam("keyword") String keyword, 
                                  Model model, 
                                  HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        List<Categorie> categories = categorieService.searchCategories(keyword);
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentUser", currentUser);
        return "categorie/list";
    }
    
    @GetMapping("/produits/categorie/{categorieId}")
    public String produitsParCategorie(@PathVariable Long categorieId, 
                                      Model model, 
                                      HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        List<Produit> produits = produitService.findByCategorieId(categorieId);
        Optional<Categorie> categorie = categorieService.getCategorieById(categorieId);
        
        model.addAttribute("produits", produits);
        categorie.ifPresent(c -> model.addAttribute("categorieNom", c.getNom()));
        model.addAttribute("currentUser", currentUser);
        return "produit/list";
    }
    
    // ==================== STATISTIQUES ====================
    
    @GetMapping("/stats")
    public String statsPage(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        long totalProduits = produitService.count();
        long totalCategories = categorieService.getAllCategories().size();
        List<Produit> produitsEnStockList = produitService.findProduitsEnStock();
        long stockCount = produitsEnStockList.size();
        List<Produit> produitsFaibleStock = produitService.findProduitsFaibleStock(10);
        
        // Calcul de la valeur totale du stock
        double valeurTotale = 0;
        List<Produit> tousProduits = produitService.findAll();
        for (Produit p : tousProduits) {
            if (p.getQuantite() > 0) {
                valeurTotale += p.getPrix() * p.getQuantite();
            }
        }
        
        // Statistiques des commandes (ajouter ces méthodes à OrderService si nécessaire)
        long totalCommandes = 0;
        long commandesEnAttente = 0;
        long commandesLivrees = 0;
        
        try {
            // Vous devrez implémenter ces méthodes dans OrderService
            // totalCommandes = orderService.count();
            // commandesEnAttente = orderService.countByStatus("EN_ATTENTE");
            // commandesLivrees = orderService.countByStatus("LIVREE");
        } catch (Exception e) {
            // Ignorer les erreurs pour les statistiques non implémentées
        }
        
        model.addAttribute("totalProduits", totalProduits);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("produitsEnStock", stockCount);
        model.addAttribute("produitsFaibleStock", produitsFaibleStock.size());
        model.addAttribute("produitsList", produitsFaibleStock);
        model.addAttribute("valeurTotale", valeurTotale);
        model.addAttribute("totalCommandes", totalCommandes);
        model.addAttribute("commandesEnAttente", commandesEnAttente);
        model.addAttribute("commandesLivrees", commandesLivrees);
        model.addAttribute("currentUser", currentUser);
        
        return "stats";
    }
    
    // ==================== PAGES EXPORT/IMPORT ====================
    
    @GetMapping("/export")
    public String exportPage(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentUser", currentUser);
        return "export";
    }
    
    @GetMapping("/import")
    public String importPage(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentUser", currentUser);
        return "import";
    }
    
    // ==================== FONCTIONS D'EXPORT ====================

    @GetMapping("/api/export/csv")
    public ResponseEntity<String> exportCSV() {
        try {
            List<Produit> produits = produitService.findAll();
            StringBuilder csv = new StringBuilder();
            
            csv.append("Nom,Description,Prix,Quantite,Categorie\n");
            
            for (Produit p : produits) {
                String categorieNom = p.getCategorie() != null ? p.getCategorie().getNom() : "";
                
                csv.append(String.format("\"%s\",\"%s\",%.2f,%d,\"%s\"\n",
                    p.getNom().replace("\"", "\"\""),
                    p.getDescription() != null ? p.getDescription().replace("\"", "\"\"") : "",
                    p.getPrix(),
                    p.getQuantite(),
                    categorieNom.replace("\"", "\"\"")
                ));
            }
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=produits.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv.toString());
                    
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/api/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            List<Produit> produits = produitService.findAll();
            StringBuilder excel = new StringBuilder();
            
            excel.append("Nom\tDescription\tPrix\tQuantite\tCategorie\n");
            
            for (Produit p : produits) {
                String categorieNom = p.getCategorie() != null ? p.getCategorie().getNom() : "";
                
                excel.append(String.format("%s\t%s\t%.2f\t%d\t%s\n",
                    p.getNom(),
                    p.getDescription() != null ? p.getDescription() : "",
                    p.getPrix(),
                    p.getQuantite(),
                    categorieNom
                ));
            }
            
            byte[] data = excel.toString().getBytes("UTF-8");
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=produits.xls")
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .body(data);
                    
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("Erreur: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/api/export/json")
    public ResponseEntity<String> exportJSON() {
        try {
            List<Produit> produits = produitService.findAll();
            List<Map<String, Object>> data = new ArrayList<>();
            
            for (Produit p : produits) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("nom", p.getNom());
                map.put("description", p.getDescription());
                map.put("prix", p.getPrix());
                map.put("quantite", p.getQuantite());
                map.put("categorie", p.getCategorie() != null ? p.getCategorie().getNom() : null);
                data.add(map);
            }
            
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(data);
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=produits.json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
                    
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/api/export/template")
    public ResponseEntity<String> downloadTemplate() {
        String template = "Nom,Description,Prix,Quantite,Categorie\n" +
                         "Produit 1,Description 1,10.50,100,Electronique\n" +
                         "Produit 2,Description 2,25.99,50,Vêtements\n" +
                         "Produit 3,Description 3,5.99,200,Alimentation";
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(template);
    }

    // ==================== FONCTIONS D'IMPORT ====================

    @PostMapping("/api/import/csv")
    public String importCSV(@RequestParam("file") MultipartFile file,
                           RedirectAttributes redirectAttributes,
                           HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Fichier vide");
                return "redirect:/import";
            }
            
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String[] lignes = content.split("\n");
            
            if (lignes.length < 2) {
                redirectAttributes.addFlashAttribute("error", "Fichier trop court");
                return "redirect:/import";
            }
            
            int importes = 0;
            int erreurs = 0;
            
            List<Categorie> toutesCategories = categorieService.getAllCategories();
            
            for (int i = 1; i < lignes.length; i++) {
                String ligne = lignes[i].trim();
                if (ligne.isEmpty()) continue;
                
                try {
                    List<String> champs = parseCSVLine(ligne);
                    
                    if (champs.size() < 4) {
                        erreurs++;
                        continue;
                    }
                    
                    Produit produit = new Produit();
                    produit.setNom(champs.get(0));
                    
                    if (champs.size() > 1) produit.setDescription(champs.get(1));
                    if (champs.size() > 2) produit.setPrix(Double.parseDouble(champs.get(2)));
                    if (champs.size() > 3) produit.setQuantite(Integer.parseInt(champs.get(3)));
                    
                    if (champs.size() > 4 && !champs.get(4).isEmpty()) {
                        String categorieNom = champs.get(4);
                        
                        boolean categorieTrouvee = false;
                        for (Categorie cat : toutesCategories) {
                            if (cat.getNom().equalsIgnoreCase(categorieNom)) {
                                produit.setCategorie(cat);
                                categorieTrouvee = true;
                                break;
                            }
                        }
                        
                        if (!categorieTrouvee) {
                            Categorie nouvelleCat = new Categorie();
                            nouvelleCat.setNom(categorieNom);
                            categorieService.saveCategorie(nouvelleCat);
                            toutesCategories.add(nouvelleCat);
                            produit.setCategorie(nouvelleCat);
                        }
                    }
                    
                    produitService.save(produit);
                    importes++;
                    
                } catch (Exception e) {
                    erreurs++;
                    System.err.println("Erreur ligne " + i + ": " + e.getMessage());
                }
            }
            
            if (importes > 0) {
                redirectAttributes.addFlashAttribute("success", importes + " produits importés");
            }
            if (erreurs > 0) {
                redirectAttributes.addFlashAttribute("warning", erreurs + " erreurs");
            }
            
            return "redirect:/produits";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/import";
        }
    }

    @PostMapping("/api/import/excel")
    public String importExcel(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "overwrite", defaultValue = "false") boolean overwrite,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        
        return importCSV(file, redirectAttributes, session);
    }

    // ==================== MÉTHODE UTILITAIRE POUR CSV ====================

    private List<String> parseCSVLine(String ligne) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder champ = new StringBuilder();
        
        for (int i = 0; i < ligne.length(); i++) {
            char c = ligne.charAt(i);
            
            if (c == '"') {
                if (i < ligne.length() - 1 && ligne.charAt(i + 1) == '"') {
                    champ.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(champ.toString().trim());
                champ = new StringBuilder();
            } else {
                champ.append(c);
            }
        }
        
        result.add(champ.toString().trim());
        
        return result;
    }
    
    // ==================== INTERCEPTEUR POUR GARDER REFERER ====================
    
    @ModelAttribute
    public void handleReferer(@RequestHeader(value = "Referer", required = false) String referer, 
                             HttpSession session) {
        if (referer != null && !referer.contains("/favoris/toggle/")) {
            session.setAttribute("referer", referer);
        }
    }
    
    // ==================== MÉTHODES UTILITAIRES POUR LES VUES ====================
    
    @ModelAttribute("cartItemCount")
    public Integer getCartItemCount(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            try {
                return panierService.countPanierItems(currentUser.getId());
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
    
    @ModelAttribute("favorisCount")
    public Integer getFavorisCount(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            try {
                List<Favori> favoris = favoriService.getUserFavoris(currentUser.getId());
                return favoris.size();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
}