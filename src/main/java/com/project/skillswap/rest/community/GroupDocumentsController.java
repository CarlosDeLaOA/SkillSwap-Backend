package com.project.skillswap.rest.community;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.skillswap.logic.entity.CommunityDocument.CommunityDocument;
import com.project.skillswap.logic.entity.CommunityDocument.CommunityDocumentRepository;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunityRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * REST Controller para gestionar documentos de grupos/comunidades
 */
@RestController
@RequestMapping("/api/group-documents")
@CrossOrigin(origins="*")
public class GroupDocumentsController {
    private static final Logger logger = LoggerFactory.getLogger(GroupDocumentsController.class);

    //#region Dependencies
    @Autowired
    private CommunityDocumentRepository documentRepository;

    @Autowired
    private LearningCommunityRepository communityRepository;

    @Autowired
    private PersonRepository personRepository;
    //#endregion

    //#region Upload Endpoint

    /**
     * POST /api/group-documents/upload
     * Sube un documento a una comunidad
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("communityId") Long communityId,
            @RequestParam(value="sessionId", required=false) Long sessionId,
            @RequestParam(value="sessionDate", required=false) String sessionDate,
            @RequestParam(value="description", required=false) String description) {
        try {
            logger.info("[GROUP-DOCS] POST /api/group-documents/upload");
            logger.info("[GROUP-DOCS] Community: "+communityId);
            logger.info("[GROUP-DOCS] File: "+file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("El archivo está vacío"));
            }

            if (!file.getContentType().equals("application/pdf")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Solo se permiten archivos PDF"));
            }

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if (communityOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            CommunityDocument doc=new CommunityDocument();
            doc.setLearningCommunity(communityOpt.get());
            doc.setTitle(file.getOriginalFilename());
            doc.setResourceUrl(file.getOriginalFilename());

            CommunityDocument saved=documentRepository.save(doc);

            Map<String, Object> response=new HashMap<>();
            response.put("success", true);
            response.put("message", "Documento subido exitosamente");
            response.put("data", formatDocument(saved));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.info("[GROUP-DOCS] Error upload: "+e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error al subir documento: "+e.getMessage()));
        }
    }

    //#endregion

    //#region Storage Endpoints

    /**
     * GET /api/group-documents/community/{communityId}/storage
     */
    @GetMapping("/community/{communityId}/storage")
    public ResponseEntity<Map<String, Object>> getCommunityStorage(
            @PathVariable Long communityId) {
        try {
            logger.info("[GROUP-DOCS] GET /api/group-documents/community/"+communityId+"/storage");

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if (communityOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            List<CommunityDocument> documents=documentRepository.findByCommunityId(communityId);

            long totalSize=0;
            for (CommunityDocument doc : documents) {
                totalSize+=1024*50;
            }

            long maxStorage=100*1024*1024;
            long availableStorage=maxStorage-totalSize;
            double usagePercentage=(totalSize*100.0)/maxStorage;

            Map<String, Object> response=new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "usedBytes", totalSize,
                    "availableBytes", availableStorage,
                    "maxBytes", maxStorage,
                    "usedFormatted", formatBytes(totalSize),
                    "availableFormatted", formatBytes(availableStorage),
                    "maxFormatted", formatBytes(maxStorage),
                    "usagePercentage", usagePercentage,
                    "documentCount", documents.size()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.info("[GROUP-DOCS] Error: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: "+e.getMessage()));
        }
    }

    /**
     * GET /api/group-documents/community/{communityId}/by-date
     */
    @GetMapping("/community/{communityId}/by-date")
    public ResponseEntity<Map<String, Object>> getCommunityDocumentsByDate(
            @PathVariable Long communityId) {
        try {
            logger.info("[GROUP-DOCS] GET /api/group-documents/community/"+communityId+"/by-date");

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if (communityOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            List<CommunityDocument> documents=documentRepository.findByCommunityId(communityId);

            Map<String, List<Map<String, Object>>> groupedByDate=new LinkedHashMap<>();
            for (CommunityDocument doc : documents) {
                String dateKey=doc.getUploadDate()!=null ?
                        new java.text.SimpleDateFormat("yyyy-MM-dd").format(doc.getUploadDate()) :
                        "Sin fecha";

                groupedByDate.computeIfAbsent(dateKey, k->new ArrayList<>())
                        .add(formatDocument(doc));
            }

            Map<String, Object> response=new HashMap<>();
            response.put("success", true);
            response.put("data", groupedByDate);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.info("[GROUP-DOCS] Error: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: "+e.getMessage()));
        }
    }

    /**
     * GET /api/group-documents/community/{communityId}/by-session
     */
    @GetMapping("/community/{communityId}/by-session")
    public ResponseEntity<Map<String, Object>> getCommunityDocumentsBySession(
            @PathVariable Long communityId) {
        try {
            logger.info("[GROUP-DOCS] GET /api/group-documents/community/"+communityId+"/by-session");

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if (communityOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            List<CommunityDocument> documents=documentRepository.findByCommunityId(communityId);

            Map<String, List<Map<String, Object>>> groupedBySession=new LinkedHashMap<>();
            for (CommunityDocument doc : documents) {
                String sessionKey=doc.getLearningSession()!=null ?
                        doc.getLearningSession().getTitle() :
                        "Material de Apoyo";

                groupedBySession.computeIfAbsent(sessionKey, k->new ArrayList<>())
                        .add(formatDocument(doc));
            }

            Map<String, Object> response=new HashMap<>();
            response.put("success", true);
            response.put("data", groupedBySession);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.info("[GROUP-DOCS] Error: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: "+e.getMessage()));
        }
    }

    /**
     * GET /api/group-documents/community/{communityId}
     */
    @GetMapping("/community/{communityId}")
    public ResponseEntity<Map<String, Object>> getDocumentsByCommunity(
            @PathVariable Long communityId) {
        try {
            logger.info("[GROUP-DOCS] GET /api/group-documents/community/"+communityId);

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if (communityOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            List<CommunityDocument> documents=documentRepository.findByCommunityId(communityId);
            List<Map<String, Object>> formattedDocs=new ArrayList<>();

            for (CommunityDocument doc : documents) {
                formattedDocs.add(formatDocument(doc));
            }

            Map<String, Object> response=new HashMap<>();
            response.put("success", true);
            response.put("data", formattedDocs);
            response.put("count", formattedDocs.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.info("[GROUP-DOCS] Error: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: "+e.getMessage()));
        }
    }

    /**
     * GET /api/group-documents/community/{communityId}/deleted
     */
    @GetMapping("/community/{communityId}/deleted")
    public ResponseEntity<Map<String, Object>> getDeletedDocuments(
            @PathVariable Long communityId) {
        try {
            logger.info("[GROUP-DOCS] GET /api/group-documents/community/"+communityId+"/deleted");

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if (communityOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            List<CommunityDocument> deletedDocuments=documentRepository.findDeletedByCommunityId(communityId);
            List<Map<String, Object>> formattedDocs=new ArrayList<>();

            for (CommunityDocument doc : deletedDocuments) {
                formattedDocs.add(formatDocument(doc));
            }

            Map<String, Object> response=new HashMap<>();
            response.put("success", true);
            response.put("data", formattedDocs);
            response.put("count", formattedDocs.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.info("[GROUP-DOCS] Error: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: "+e.getMessage()));
        }
    }

    /**
     * GET /api/group-documents/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response=new HashMap<>();
        response.put("status", "ok");
        response.put("service", "group-documents");
        response.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    //#endregion

    //#region Private Methods

    /**
     * Formatea un documento para respuesta
     */
    private Map<String, Object> formatDocument(CommunityDocument doc) {
        Map<String, Object> map=new HashMap<>();
        map.put("id", doc.getId());
        map.put("title", doc.getTitle());
        map.put("originalFileName", doc.getTitle());
        map.put("resourceUrl", doc.getResourceUrl());
        map.put("uploadDate", doc.getUploadDate());
        map.put("sessionDate", doc.getUploadDate());
        map.put("fileSize", 1024*50);
        map.put("formattedFileSize", "50 KB");
        map.put("description", "");

        if (doc.getLearningSession()!=null) {
            Map<String, Object> session=new HashMap<>();
            session.put("id", doc.getLearningSession().getId());
            session.put("title", doc.getLearningSession().getTitle());
            map.put("learningSession", session);
        } else {
            map.put("learningSession", null);
        }

        if (doc.getUploadedBy()!=null) {
            Map<String, Object> uploader=new HashMap<>();
            uploader.put("id", doc.getUploadedBy().getId());
            uploader.put("fullName", doc.getUploadedBy().getPerson().getFullName());
            uploader.put("profilePhotoUrl", doc.getUploadedBy().getPerson().getProfilePhotoUrl());
            map.put("uploadedBy", uploader);
        }

        return map;
    }

    /**
     * Formatea bytes a formato legible
     */
    private String formatBytes(long bytes) {
        if (bytes<1024) return bytes+" B";
        int z=(63-Long.numberOfLeadingZeros(bytes))/10;
        return String.format("%.1f %sB", (double)bytes/(1L<<(z*10)), " KMGTPE".charAt(z));
    }

    /**
     * Crea respuesta de error
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response=new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    //#endregion
}