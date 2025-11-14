package com.project.skillswap.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Service para manejar operaciones con Cloudinary
 * Proporciona métodos para subir, eliminar y gestionar imágenes
 *
 * @author SkillSwap Team
 * @version 1.0.0
 */
@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Sube una imagen a Cloudinary
     *
     * @param file Archivo a subir
     * @return URL segura de la imagen subida
     * @throws IOException si hay error en la subida
     */
    public String uploadImage(MultipartFile file) throws IOException {
        System.out.println("📤 [CloudinaryService] Uploading image: " + file.getOriginalFilename());

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "skillswap/profile_photos",
                        "resource_type", "image",
                        "transformation", "c_fill,g_face,h_500,w_500"
                ));

        String imageUrl = uploadResult.get("secure_url").toString();
        System.out.println("✅ [CloudinaryService] Image uploaded successfully: " + imageUrl);

        return imageUrl;
    }

    /**
     * Elimina una imagen de Cloudinary usando su public_id
     *
     * @param publicId ID público de la imagen en Cloudinary
     * @throws IOException si hay error en la eliminación
     */
    public void deleteImage(String publicId) throws IOException {
        System.out.println("🗑️ [CloudinaryService] Deleting image with public_id: " + publicId);
        Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        System.out.println("✅ [CloudinaryService] Image deleted successfully: " + result);
    }

    /**
     * Extrae el public_id de una URL de Cloudinary
     *
     * @param imageUrl URL completa de Cloudinary
     * @return public_id extraído o null si la URL no es válida
     */
    public String extractPublicIdFromUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            return null;
        }

        try {
            // Ejemplo: https://res.cloudinary.com/cloud/image/upload/v123/folder/image.jpg
            // Extraemos: folder/image
            String[] parts = imageUrl.split("/upload/");
            if (parts.length < 2) return null;

            String pathAfterUpload = parts[1];
            // Removemos la versión (v123456/)
            String withoutVersion = pathAfterUpload.replaceFirst("v\\d+/", "");
            // Removemos la extensión
            String publicId = withoutVersion.replaceFirst("\\.[^.]+$", "");

            System.out.println("🔍 [CloudinaryService] Extracted public_id: " + publicId + " from URL: " + imageUrl);

            return publicId;
        } catch (Exception e) {
            System.err.println("❌ [CloudinaryService] Error extracting public_id: " + e.getMessage());
            return null;
        }
    }
}