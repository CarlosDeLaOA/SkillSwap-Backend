package com.project.skillswap.logic.entity.GroupSessionDocument;

import com.project.skillswap.config.CloudinaryService;
import com.project.skillswap.logic.entity.CommunityMember.CommunityMemberRepository;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunityRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar documentos de sesiones grupales.
 * Usa Cloudinary para almacenamiento de PDFs.
 */
@Service
public class GroupSessionDocumentService {

    //#region Constants
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final long MAX_STORAGE_BYTES = 100L * 1024L * 1024L; // 100MB
    //#endregion

    //#region Dependencies
    @Autowired
    private GroupSessionDocumentRepository documentRepository;

    @Autowired
    private LearningCommunityRepository communityRepository;

    @Autowired
    private LearningSessionRepository sessionRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CommunityMemberRepository memberRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired(required = false)
    private EmailDocumentNotificationService emailNotificationService;
    //#endregion

    //#region Public Methods - Upload

    /**
     * Sube un documento PDF a Cloudinary
     */
    @Transactional
    public GroupSessionDocument uploadDocument(MultipartFile file, Long communityId, Long sessionId,
                                               Long personId, LocalDateTime sessionDate, String description) {
        // Validar que el archivo no esté vacío
        if (file.isEmpty()) {
            throw new InvalidDocumentFormatException("El archivo está vacío");
        }

        // Validar formato PDF
        validatePdfFormat(file);

        // Obtener entidades
        LearningCommunity community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Comunidad no encontrada: " + communityId));

        // Sesión es opcional
        LearningSession session = null;
        if (sessionId != null) {
            session = sessionRepository.findById(sessionId).orElse(null);
        }

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + personId));

        // Validar que el usuario sea miembro de la comunidad
        validateMembership(personId, communityId, "subir");

        // Validar límite de almacenamiento
        validateStorageLimit(communityId, file.getSize());

        // Subir archivo a Cloudinary
        String cloudinaryUrl;
        try {
            cloudinaryUrl = cloudinaryService.uploadPdf(file, communityId);
        } catch (IOException e) {
            throw new RuntimeException("Error al subir el archivo a Cloudinary: " + e.getMessage(), e);
        }

        // Crear entidad
        GroupSessionDocument document = new GroupSessionDocument();
        document.setLearningCommunity(community);
        document.setLearningSession(session);
        document.setUploadedBy(person);
        document.setFileName(UUID.randomUUID().toString() + ".pdf");
        document.setOriginalFileName(file.getOriginalFilename());
        document.setFilePath(cloudinaryUrl); // Guardamos la URL de Cloudinary
        document.setFileSize(file.getSize());
        document.setContentType(file.getContentType());
        document.setSessionDate(sessionDate != null ? sessionDate : LocalDateTime.now());
        document.setDescription(description);
        document.setActive(true);

        GroupSessionDocument savedDocument = documentRepository.save(document);

        // Notificar a los miembros por email
        notifyMembersNewDocument(savedDocument);

        System.out.println("Documento subido exitosamente a Cloudinary: " + savedDocument.getOriginalFileName());
        System.out.println("URL: " + cloudinaryUrl);

        return savedDocument;
    }

    //#endregion

    //#region Public Methods - Retrieval

    /**
     * Obtiene todos los documentos de una comunidad organizados por fecha descendente.
     */
    public List<GroupSessionDocument> getDocumentsByCommunity(Long communityId, Long personId) {
        validateMembership(personId, communityId, "visualizar");
        return documentRepository.findByCommunityIdOrderBySessionDateDesc(communityId);
    }

    /**
     * Obtiene documentos de una comunidad agrupados por sesión.
     */
    public Map<String, List<GroupSessionDocument>> getDocumentsGroupedBySession(Long communityId, Long personId) {
        validateMembership(personId, communityId, "visualizar");

        List<GroupSessionDocument> allDocuments = documentRepository.findByCommunityIdOrderBySessionDateDesc(communityId);

        Map<String, List<GroupSessionDocument>> grouped = new LinkedHashMap<>();

        for (GroupSessionDocument doc : allDocuments) {
            String key;
            if (doc.getLearningSession() != null) {
                key = doc.getLearningSession().getTitle();
            } else {
                key = " Material de Apoyo";
            }

            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(doc);
        }

        return grouped;
    }

    /**
     * Obtiene documentos de una comunidad agrupados por fecha.
     */
    public Map<String, List<GroupSessionDocument>> getDocumentsGroupedByDate(Long communityId, Long personId) {
        validateMembership(personId, communityId, "visualizar");

        List<GroupSessionDocument> allDocuments = documentRepository.findByCommunityIdOrderBySessionDateDesc(communityId);

        return allDocuments.stream()
                .collect(Collectors.groupingBy(
                        doc -> doc.getSessionDate().toLocalDate().toString(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * Obtiene documentos de una sesión específica.
     */
    public List<GroupSessionDocument> getDocumentsBySession(Long sessionId, Long personId, Long communityId) {
        validateMembership(personId, communityId, "visualizar");
        return documentRepository.findBySessionIdOrderByUploadDateDesc(sessionId);
    }

    /**
     * Obtiene documentos de material de apoyo (sin sesión asociada).
     */
    public List<GroupSessionDocument> getSupportMaterials(Long communityId, Long personId) {
        validateMembership(personId, communityId, "visualizar");
        return documentRepository.findSupportMaterialsByCommunityId(communityId);
    }

    /**
     * Obtiene un documento por su ID.
     */
    public GroupSessionDocument getDocumentById(Long documentId, Long personId) {
        GroupSessionDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + documentId));

        validateMembership(personId, document.getLearningCommunity().getId(), "visualizar");

        return document;
    }

    /**
     * Obtiene documentos borrados de una comunidad.
     */
    public List<GroupSessionDocument> getDeletedDocuments(Long communityId, Long personId) {
        validateMembership(personId, communityId, "visualizar");
        return documentRepository.findDeletedByCommunityId(communityId);
    }

    /**
     * Descarga el contenido de un documento desde Cloudinary.
     *
     * @param documentId ID del documento
     * @param personId ID de la persona que solicita
     * @return bytes del archivo
     */
    public byte[] downloadDocument(Long documentId, Long personId) {
        GroupSessionDocument document = getDocumentById(documentId, personId);

        try {
            URL url = new URL(document.getFilePath());
            try (InputStream in = url.openStream()) {
                return in.readAllBytes();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al descargar el documento: " + document.getOriginalFileName(), e);
        }
    }

    //#endregion

    //#region Public Methods - Storage Stats

    /**
     * Obtiene estadísticas de almacenamiento de una comunidad.
     */
    public Map<String, Object> getStorageStats(Long communityId) {
        Long usedBytes = documentRepository.getTotalStorageByCommunityId(communityId);
        if (usedBytes == null) usedBytes = 0L;

        Long documentCount = documentRepository.countDocumentsByCommunityId(communityId);
        Long availableBytes = MAX_STORAGE_BYTES - usedBytes;

        Map<String, Object> stats = new HashMap<>();
        stats.put("usedBytes", usedBytes);
        stats.put("availableBytes", availableBytes);
        stats.put("maxBytes", MAX_STORAGE_BYTES);
        stats.put("usedFormatted", formatBytes(usedBytes));
        stats.put("availableFormatted", formatBytes(availableBytes));
        stats.put("maxFormatted", "100 MB");
        stats.put("usagePercentage", (usedBytes * 100.0) / MAX_STORAGE_BYTES);
        stats.put("documentCount", documentCount);

        return stats;
    }

    //#endregion

    //#region Public Methods - Delete

    /**
     * Elimina (soft delete) un documento con razón y registro.
     */
    @Transactional
    public void deleteDocument(Long documentId, Long personId, String reason) {
        GroupSessionDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + documentId));

        validateMembership(personId, document.getLearningCommunity().getId(), "eliminar");

        Person deletedBy = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + personId));

        // Registrar información de borrado
        document.setActive(false);
        document.setDeletedAt(LocalDateTime.now());
        document.setDeletedBy(deletedBy);
        document.setDeletionReason(reason);

        documentRepository.save(document);

        System.out.println("Documento eliminado por " + deletedBy.getFullName() + ": " + document.getOriginalFileName());
        System.out.println("Razón: " + reason);
    }

    //#endregion

    //#region Private Methods - Validation

    /**
     * Valida que el archivo sea un PDF válido.
     */
    private void validatePdfFormat(MultipartFile file) {
        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename();

        if (contentType == null || !contentType.equalsIgnoreCase(ALLOWED_CONTENT_TYPE)) {
            throw new InvalidDocumentFormatException(originalFileName, contentType);
        }

        if (originalFileName != null && ! originalFileName.toLowerCase().endsWith(".pdf")) {
            throw new InvalidDocumentFormatException(originalFileName, contentType);
        }
    }

    /**
     * Valida que no se exceda el límite de almacenamiento del grupo.
     */
    private void validateStorageLimit(Long communityId, Long fileSize) {
        Long currentStorage = documentRepository.getTotalStorageByCommunityId(communityId);
        if (currentStorage == null) currentStorage = 0L;

        if (currentStorage + fileSize > MAX_STORAGE_BYTES) {
            throw new StorageLimitExceededException(communityId, currentStorage, fileSize);
        }
    }

    /**
     * Valida que el usuario sea miembro de la comunidad.
     */
    private void validateMembership(Long personId, Long communityId, String action) {
        List<com.project.skillswap.logic.entity.CommunityMember.CommunityMember> memberships =
                memberRepository.findActiveMembersByPersonId(personId);

        boolean isMember = memberships.stream()
                .anyMatch(m -> m.getLearningCommunity().getId().equals(communityId));

        if (!isMember) {
            throw new DocumentAccessDeniedException(personId, communityId, action);
        }
    }

    //#endregion

    //#region Private Methods - Notifications

    /**
     * Notifica a los miembros de la comunidad sobre un nuevo documento.
     */
    private void notifyMembersNewDocument(GroupSessionDocument document) {
        if (emailNotificationService != null) {
            try {
                emailNotificationService.sendNewDocumentNotification(document);
            } catch (Exception e) {
                System.err.println("Error al enviar notificaciones de documento: " + e.getMessage());
            }
        }
    }

    //#endregion

    //#region Private Methods - Utilities

    /**
     * Formatea bytes a formato legible.
     */
    private String formatBytes(Long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }

    //#endregion
}