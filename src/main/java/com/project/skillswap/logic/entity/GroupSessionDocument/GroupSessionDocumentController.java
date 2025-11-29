package com.project.skillswap.logic.entity.GroupSessionDocument;

import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestionar documentos de sesiones grupales.
 * Permite subir, descargar, listar y eliminar documentos PDF.
 */
@RestController
@RequestMapping("/group-documents")
@PreAuthorize("isAuthenticated()")
public class GroupSessionDocumentController {

    @Autowired
    private GroupSessionDocumentService documentService;

    //#region Upload Endpoints

    /**
     * Sube un documento PDF a una comunidad.
     * POST /group-documents/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("communityId") Long communityId,
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestParam(value = "sessionDate", required = false) String sessionDate,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal Person person) {

        Map<String, Object> response = new HashMap<>();

        try {
            LocalDateTime parsedSessionDate = null;
            if (sessionDate != null && !sessionDate.isEmpty()) {
                parsedSessionDate = LocalDateTime.parse(sessionDate);
            }

            GroupSessionDocument document = documentService.uploadDocument(
                    file,
                    communityId,
                    sessionId,
                    person.getId(),
                    parsedSessionDate,
                    description
            );

            response.put("success", true);
            response.put("message", "Documento subido exitosamente");
            response.put("data", document);

            return ResponseEntity.ok(response);

        } catch (InvalidDocumentFormatException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (StorageLimitExceededException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);

        } catch (DocumentAccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al subir el documento: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    //#endregion

    //#region List Endpoints

    /**
     * Obtiene todos los documentos de una comunidad.
     * GET /group-documents/community/{communityId}
     */
    @GetMapping("/community/{communityId}")
    public ResponseEntity<Map<String, Object>> getDocumentsByCommunity(
            @PathVariable Long communityId,
            @AuthenticationPrincipal Person person) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<GroupSessionDocument> documents =
                    documentService.getDocumentsByCommunity(communityId, person.getId());

            response.put("success", true);
            response.put("data", documents);
            response.put("count", documents.size());

            return ResponseEntity.ok(response);

        } catch (DocumentAccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener documentos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene documentos agrupados por sesión.
     * GET /group-documents/community/{communityId}/by-session
     */
    @GetMapping("/community/{communityId}/by-session")
    public ResponseEntity<Map<String, Object>> getDocumentsGroupedBySession(
            @PathVariable Long communityId,
            @AuthenticationPrincipal Person person) {

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, List<GroupSessionDocument>> groupedDocuments =
                    documentService.getDocumentsGroupedBySession(communityId, person.getId());

            response.put("success", true);
            response.put("data", groupedDocuments);

            return ResponseEntity.ok(response);

        } catch (DocumentAccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener documentos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene documentos agrupados por fecha.
     * GET /group-documents/community/{communityId}/by-date
     */
    @GetMapping("/community/{communityId}/by-date")
    public ResponseEntity<Map<String, Object>> getDocumentsGroupedByDate(
            @PathVariable Long communityId,
            @AuthenticationPrincipal Person person) {

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, List<GroupSessionDocument>> groupedDocuments =
                    documentService.getDocumentsGroupedByDate(communityId, person.getId());

            response.put("success", true);
            response.put("data", groupedDocuments);

            return ResponseEntity.ok(response);

        } catch (DocumentAccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener documentos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene documentos de una sesión específica.
     * GET /group-documents/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getDocumentsBySession(
            @PathVariable Long sessionId,
            @RequestParam Long communityId,
            @AuthenticationPrincipal Person person) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<GroupSessionDocument> documents =
                    documentService.getDocumentsBySession(sessionId, person.getId(), communityId);

            response.put("success", true);
            response.put("data", documents);
            response.put("count", documents.size());

            return ResponseEntity.ok(response);

        } catch (DocumentAccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener documentos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene documentos borrados de una comunidad.
     * GET /group-documents/community/{communityId}/deleted
     */
    @GetMapping("/community/{communityId}/deleted")
    public ResponseEntity<Map<String, Object>> getDeletedDocuments(
            @PathVariable Long communityId,
            @AuthenticationPrincipal Person person) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<GroupSessionDocument> documents =
                    documentService.getDeletedDocuments(communityId, person.getId());

            response.put("success", true);
            response.put("data", documents);
            response.put("count", documents.size());

            return ResponseEntity.ok(response);

        } catch (DocumentAccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener documentos borrados: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    //#endregion

    //#region Download Endpoints

    /**
     * Descarga un documento.
     * GET /group-documents/{documentId}/download
     */
    @GetMapping("/{documentId}/download")
    public ResponseEntity<? > downloadDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal Person person) {

        try {
            GroupSessionDocument document = documentService.getDocumentById(documentId, person.getId());
            byte[] fileContent = documentService.downloadDocument(documentId, person.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", document.getOriginalFileName());
            headers.setContentLength(fileContent.length);

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (DocumentAccessDeniedException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al descargar el documento: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Visualiza un documento en el navegador.
     * GET /group-documents/{documentId}/view
     */
    @GetMapping("/{documentId}/view")
    public ResponseEntity<?> viewDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal Person person) {

        try {
            GroupSessionDocument document = documentService.getDocumentById(documentId, person.getId());
            byte[] fileContent = documentService.downloadDocument(documentId, person.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", document.getOriginalFileName());
            headers.setContentLength(fileContent.length);

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (DocumentAccessDeniedException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al visualizar el documento: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene la URL directa del documento en Cloudinary.
     * GET /group-documents/{documentId}/url
     */
    @GetMapping("/{documentId}/url")
    public ResponseEntity<Map<String, Object>> getDocumentUrl(
            @PathVariable Long documentId,
            @AuthenticationPrincipal Person person) {

        Map<String, Object> response = new HashMap<>();

        try {
            GroupSessionDocument document = documentService.getDocumentById(documentId, person.getId());

            response.put("success", true);
            response.put("url", document.getFilePath());
            response.put("fileName", document.getOriginalFileName());

            return ResponseEntity.ok(response);

        } catch (DocumentAccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener URL del documento: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    //#endregion

    //#region Storage Endpoints

    /**
     * Obtiene estadísticas de almacenamiento de una comunidad.
     * GET /group-documents/community/{communityId}/storage
     */
    @GetMapping("/community/{communityId}/storage")
    public ResponseEntity<Map<String, Object>> getStorageStats(
            @PathVariable Long communityId,
            @AuthenticationPrincipal Person person) {

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> stats = documentService.getStorageStats(communityId);

            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener estadísticas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    //#endregion

    //#region Delete Endpoints

    /**
     * Elimina un documento (soft delete) con razón.
     * DELETE /group-documents/{documentId}
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @PathVariable Long documentId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Person person) {

        Map<String, Object> response = new HashMap<>();

        try {
            String reason = (body != null && body.get("reason") != null)
                    ? body.get("reason")
                    : "Sin razón especificada";

            documentService.deleteDocument(documentId, person.getId(), reason);

            response.put("success", true);
            response.put("message", "Documento eliminado exitosamente");

            return ResponseEntity.ok(response);

        } catch (DocumentAccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar el documento: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    //#endregion
}