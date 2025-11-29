package com.project.skillswap.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Service para manejar operaciones con Cloudinary
 * Proporciona métodos para subir, eliminar y gestionar imágenes y documentos
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
        System.out.println("[CloudinaryService] Uploading image: " + file.getOriginalFilename());

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "skillswap/profile_photos",
                        "resource_type", "image",
                        "transformation", "c_fill,g_face,h_500,w_500"
                ));

        String imageUrl = uploadResult.get("secure_url").toString();
        System.out.println("[CloudinaryService] Image uploaded successfully: " + imageUrl);

        return imageUrl;
    }

    /**
     * Sube un documento PDF a Cloudinary usando el preset skillswap_pdfs
     * Los archivos se organizan por comunidad en carpetas separadas
     *
     * @param file Archivo PDF a subir
     * @param communityId ID de la comunidad para organizar en carpetas
     * @return URL segura del documento subido
     * @throws IOException si hay error en la subida
     */
    public String uploadPdf(MultipartFile file, Long communityId) throws IOException {
        System.out.println("[CloudinaryService] Uploading PDF: " + file.getOriginalFilename());
        System.out.println("[CloudinaryService] File size: " + file.getSize() + " bytes");

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "upload_preset", "skillswap_pdfs",
                        "folder", "skillswap/documents/community_" + communityId,
                        "resource_type", "raw",
                        "type", "upload"
                ));

        String pdfUrl = uploadResult.get("secure_url").toString();
        String publicId = uploadResult.get("public_id").toString();

        System.out.println("[CloudinaryService] PDF uploaded successfully");
        System.out.println("[CloudinaryService] URL: " + pdfUrl);
        System.out.println("[CloudinaryService] Public ID: " + publicId);
        System.out.println("[CloudinaryService] Type: " + uploadResult.getOrDefault("type", "N/A"));
        System.out.println("[CloudinaryService] Access mode: " + uploadResult.getOrDefault("access_mode", "N/A"));

        return pdfUrl;
    }

    /**
     * Descarga un PDF desde Cloudinary como bytes
     * Intenta primero con URL directa, si falla usa URL firmada
     *
     * @param cloudinaryUrl URL del archivo en Cloudinary
     * @return Bytes del archivo PDF
     * @throws IOException si hay error en la descarga
     */
    public byte[] downloadPdfFromCloudinary(String cloudinaryUrl) throws IOException {
        System.out.println("[CloudinaryService] Downloading PDF from: " + cloudinaryUrl);

        try {
            URL url = new URL(cloudinaryUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            System.out.println("[CloudinaryService] Response code: " + responseCode);

            if (responseCode == 200) {
                try (InputStream in = connection.getInputStream()) {
                    byte[] fileBytes = in.readAllBytes();
                    System.out.println("[CloudinaryService] Downloaded " + fileBytes.length + " bytes");
                    return fileBytes;
                }
            } else if (responseCode == 401 || responseCode == 404) {
                System.out.println("[CloudinaryService] Direct URL failed, trying signed URL...");
                return downloadWithSignedUrl(cloudinaryUrl);
            } else {
                throw new IOException("HTTP error code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("[CloudinaryService] Error downloading PDF: " + e.getMessage());
            throw new IOException("Error al descargar el documento desde Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Intenta descargar usando URL firmada como fallback
     */
    private byte[] downloadWithSignedUrl(String cloudinaryUrl) throws IOException {
        try {
            String publicId = extractPublicIdFromCloudinaryUrl(cloudinaryUrl);
            if (publicId == null) {
                throw new IOException("No se pudo extraer public_id de la URL");
            }

            System.out.println("[CloudinaryService] Extracted public_id: " + publicId);

            String signedUrl = cloudinary.url()
                    .resourceType("raw")
                    .type("upload")
                    .signed(true)
                    .generate(publicId);

            System.out.println("[CloudinaryService] Generated signed URL");

            URL url = new URL(signedUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            System.out.println("[CloudinaryService] Signed URL response code: " + responseCode);

            if (responseCode == 200) {
                try (InputStream in = connection.getInputStream()) {
                    byte[] fileBytes = in.readAllBytes();
                    System.out.println("[CloudinaryService] Downloaded " + fileBytes.length + " bytes with signed URL");
                    return fileBytes;
                }
            } else {
                throw new IOException("Signed URL also failed with code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("[CloudinaryService] Error with signed URL: " + e.getMessage());
            throw new IOException("Error descargando con URL firmada: " + e.getMessage(), e);
        }
    }

    /**
     * Extrae el public_id de una URL de Cloudinary
     *
     * @param cloudinaryUrl URL completa de Cloudinary
     * @return public_id extraído o null si la URL no es válida
     */
    private String extractPublicIdFromCloudinaryUrl(String cloudinaryUrl) {
        if (cloudinaryUrl == null || ! cloudinaryUrl.contains("cloudinary.com")) {
            return null;
        }

        try {
            String[] parts = cloudinaryUrl.split("/upload/");
            if (parts.length < 2) return null;

            String pathAfterUpload = parts[1];
            String withoutVersion = pathAfterUpload.replaceFirst("v\\d+/", "");
            String publicId = withoutVersion.replaceFirst("\\.[^.]+$", "");

            return publicId;
        } catch (Exception e) {
            System.err.println("[CloudinaryService] Error extracting public_id: " + e.getMessage());
            return null;
        }
    }

    /**
     * Elimina una imagen de Cloudinary usando su public_id
     *
     * @param publicId ID público de la imagen en Cloudinary
     * @throws IOException si hay error en la eliminación
     */
    public void deleteImage(String publicId) throws IOException {
        System.out.println("[CloudinaryService] Deleting image with public_id: " + publicId);
        Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        System.out.println("[CloudinaryService] Image deleted successfully: " + result);
    }

    /**
     * Elimina un documento PDF de Cloudinary usando su public_id
     * Incluye invalidación de cache para asegurar que el archivo se elimine del CDN
     *
     * @param publicId ID público del documento en Cloudinary
     * @throws IOException si hay error en la eliminación
     */
    public void deletePdf(String publicId) throws IOException {
        System.out.println("[CloudinaryService] Deleting PDF with public_id: " + publicId);
        Map result = cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap(
                        "resource_type", "raw",
                        "invalidate", true
                ));
        System.out.println("[CloudinaryService] PDF deleted successfully: " + result);
    }

    /**
     * Extrae el public_id de una URL de Cloudinary
     * El public_id es necesario para operaciones de eliminación y transformación
     *
     * @param imageUrl URL completa de Cloudinary
     * @return public_id extraído o null si la URL no es válida
     */
    public String extractPublicIdFromUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            return null;
        }

        try {
            String[] parts = imageUrl.split("/upload/");
            if (parts.length < 2) return null;

            String pathAfterUpload = parts[1];
            String withoutVersion = pathAfterUpload.replaceFirst("v\\d+/", "");
            String publicId = withoutVersion.replaceFirst("\\.[^.]+$", "");

            System.out.println("[CloudinaryService] Extracted public_id: " + publicId + " from URL: " + imageUrl);

            return publicId;
        } catch (Exception e) {
            System.err.println("[CloudinaryService] Error extracting public_id: " + e.getMessage());
            return null;
        }
    }
}