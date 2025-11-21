package com.project.skillswap.logic.entity.videocall;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para manejar documentos de sesiones
 */
@Service
public class SessionDocumentService {

    private static final String UPLOAD_DIR = "uploads/session-documents/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png"
    );

    // Almacenamiento en memoria (en producción usar base de datos)
    private final Map<Long, SessionDocumentInfo> documents = new ConcurrentHashMap<>();
    private Long documentIdCounter = 1L;

    /**
     * Obtiene la lista de documentos de una sesión
     */
    public List<Map<String, Object>> getSessionDocuments(Long sessionId) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (SessionDocumentInfo doc : documents.values()) {
            if (doc.getSessionId().equals(sessionId)) {
                Map<String, Object> docData = new HashMap<>();
                docData.put("id", doc.getId());
                docData.put("name", doc.getName());
                docData.put("type", doc.getType());
                docData.put("size", doc.getSize());
                docData.put("uploadedBy", doc.getUploadedBy());
                docData.put("uploadedAt", doc.getUploadedAt());
                result.add(docData);
            }
        }

        return result;
    }

    /**
     * Sube un documento a una sesión
     */
    public Map<String, Object> uploadDocument(Long sessionId, Long personId, MultipartFile file) {
        // Validaciones
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido (10MB)");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Tipo de archivo no permitido");
        }


        // virusScanService.scan(file);

        try {
            // Crear directorio si no existe
            Path uploadPath = Paths.get(UPLOAD_DIR + sessionId);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generar nombre único
            String originalFilename = file.getOriginalFilename();
            String filename = System.currentTimeMillis() + "_" + originalFilename;
            Path filePath = uploadPath.resolve(filename);

            // Guardar archivo
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Crear registro
            Long documentId = documentIdCounter++;
            SessionDocumentInfo documentInfo = new SessionDocumentInfo(
                    documentId,
                    sessionId,
                    personId,
                    originalFilename,
                    file.getContentType(),
                    file.getSize(),
                    filePath.toString()
            );

            documents.put(documentId, documentInfo);

            System.out.println("📄 Documento subido exitosamente");
            System.out.println("   Sesión: " + sessionId);
            System.out.println("   Nombre: " + originalFilename);
            System.out.println("   Tamaño: " + file.getSize() + " bytes");


            // notificationService.notifyDocumentUpload(sessionId, documentInfo);

            Map<String, Object> result = new HashMap<>();
            result.put("id", documentInfo.getId());
            result.put("name", documentInfo.getName());
            result.put("type", documentInfo.getType());
            result.put("size", documentInfo.getSize());
            result.put("uploadedBy", "Usuario");
            result.put("uploadedAt", documentInfo.getUploadedAt());

            return result;

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo: " + e.getMessage());
        }
    }

    /**
     * Descarga un documento
     */
    public ResponseEntity<Resource> downloadDocument(Long documentId) {
        try {
            SessionDocumentInfo doc = documents.get(documentId);
            if (doc == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(doc.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = doc.getType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Elimina un documento
     */
    public void deleteDocument(Long documentId, Long personId, boolean isModerator) {
        SessionDocumentInfo doc = documents.get(documentId);

        if (doc == null) {
            throw new IllegalArgumentException("Documento no encontrado");
        }

        // Verificar permisos
        if (!isModerator && !doc.getUploadedById().equals(personId)) {
            throw new IllegalArgumentException("No tienes permisos para eliminar este documento");
        }

        try {
            // Eliminar archivo físico
            Path filePath = Paths.get(doc.getFilePath());
            Files.deleteIfExists(filePath);

            // Eliminar registro
            documents.remove(documentId);

            System.out.println("🗑 Documento eliminado: " + doc.getName());

        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar el archivo: " + e.getMessage());
        }
    }

    /**
     * Clase interna para representar información de un documento
     */
    private static class SessionDocumentInfo {
        private final Long id;
        private final Long sessionId;
        private final Long uploadedById;
        private final String name;
        private final String type;
        private final Long size;
        private final String filePath;
        private final Date uploadedAt;

        public SessionDocumentInfo(Long id, Long sessionId, Long uploadedById, String name,
                                   String type, Long size, String filePath) {
            this.id = id;
            this.sessionId = sessionId;
            this.uploadedById = uploadedById;
            this.name = name;
            this.type = type;
            this.size = size;
            this.filePath = filePath;
            this.uploadedAt = new Date();
        }

        public Long getId() { return id; }
        public Long getSessionId() { return sessionId; }
        public Long getUploadedById() { return uploadedById; }
        public String getName() { return name; }
        public String getType() { return type; }
        public Long getSize() { return size; }
        public String getFilePath() { return filePath; }
        public Date getUploadedAt() { return uploadedAt; }
        public String getUploadedBy() { return "Usuario " + uploadedById; }
    }
}