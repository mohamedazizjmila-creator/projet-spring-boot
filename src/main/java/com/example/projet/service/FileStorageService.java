package com.example.projet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    /**
     * Sauvegarde un fichier image sur le disque
     */
    public String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        // Créer le dossier s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Valider le type de fichier
        if (!isImageFile(file)) {
            throw new IOException("Le fichier n'est pas une image valide");
        }
        
        // Générer un nom unique pour le fichier
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        // Générer un nom unique avec timestamp et UUID
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String uniqueFileName = "produit_" + timestamp + "_" + uuid + fileExtension;
        
        Path filePath = uploadPath.resolve(uniqueFileName);
        
        // Copier le fichier
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return uniqueFileName;
    }
    
    /**
     * NOUVELLE MÉTHODE : Télécharge et sauvegarde une image depuis une URL
     */
    public String downloadAndSaveImageFromUrl(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        
        // Créer le dossier s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Déterminer l'extension depuis l'URL
        String fileExtension = ".jpg";
        if (imageUrl.contains(".")) {
            String ext = imageUrl.substring(imageUrl.lastIndexOf("."));
            if (ext.length() <= 5 && !ext.contains("?")) {
                fileExtension = ext.split("\\?")[0];
            }
        }
        
        // Générer un nom unique
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String uniqueFileName = "url_" + timestamp + "_" + uuid + fileExtension;
        
        Path filePath = uploadPath.resolve(uniqueFileName);
        
        // Télécharger l'image depuis l'URL
        try (InputStream in = new URL(imageUrl).openStream()) {
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        return uniqueFileName;
    }
    
    /**
     * Supprime un fichier image
     */
    public void deleteImage(String fileName) throws IOException {
        if (fileName != null && !fileName.isEmpty()) {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        }
    }
    
    /**
     * Vérifie si le fichier est une image
     */
    public boolean isImageFile(MultipartFile file) {
        if (file == null) {
            return false;
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        
        return contentType.startsWith("image/");
    }
    
    /**
     * Vérifie la taille du fichier (max 5MB)
     */
    public boolean isFileSizeValid(MultipartFile file) {
        if (file == null) {
            return false;
        }
        
        long fileSize = file.getSize();
        long maxSize = 5 * 1024 * 1024; // 5MB
        
        return fileSize <= maxSize;
    }
    
    /**
     * Obtient l'URL complète pour accéder à l'image
     */
    public String getImageUrl(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        return "/uploads/" + fileName;
    }
}