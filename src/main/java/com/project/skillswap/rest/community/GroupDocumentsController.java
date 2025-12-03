package com.project.skillswap.rest.community;

import com.cloudinary.Cloudinary;
import com.project.skillswap.logic.entity.CommunityDocument.CommunityDocument;
import com.project.skillswap.logic.entity.CommunityDocument.CommunityDocumentRepository;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunityRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST Controller para gestionar documentos de grupos/comunidades
 */
@RestController
@RequestMapping("/api/group-documents")
@CrossOrigin(origins="*")
public class GroupDocumentsController {

    //#region Dependencies
    @Autowired
    private CommunityDocumentRepository documentRepository;

    @Autowired
    private LearningCommunityRepository communityRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private Cloudinary cloudinary;
    //#endregion

    //#region Upload Endpoint

    /**
     * POST /api/group-documents/upload
     * Sube un documento a Cloudinary y guarda la referencia en BD
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String,Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("communityId") Long communityId,
            @RequestParam(value="sessionId",required=false) Long sessionId,
            @RequestParam(value="sessionDate",required=false) String sessionDate,
            @RequestParam(value="description",required=false) String description) {
        try {
            System.out.println("[GROUP-DOCS] POST /api/group-documents/upload");
            System.out.println("[GROUP-DOCS] Community: "+communityId);
            System.out.println("[GROUP-DOCS] File: "+file.getOriginalFilename());

            if(file.isEmpty()){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("El archivo está vacío"));
            }

            if(!file.getContentType().equals("application/pdf")){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Solo se permiten archivos PDF"));
            }

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if(communityOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            System.out.println("[GROUP-DOCS] Subiendo archivo a Cloudinary...");

            String originalFilename=file.getOriginalFilename();
            String safeFilename=originalFilename.replaceAll("[^a-zA-Z0-9._-]","_");

            Map<String,Object> uploadParams=new HashMap<>();
            uploadParams.put("resource_type","raw");
            uploadParams.put("folder","community-documents/community-"+communityId);
            uploadParams.put("public_id",safeFilename.replace(".pdf","")+"_"+System.currentTimeMillis());
            uploadParams.put("use_filename",true);

            Map<?,?> uploadResult=cloudinary.uploader().upload(file.getBytes(),uploadParams);
            String cloudinaryUrl=(String)uploadResult.get("secure_url");

            System.out.println("[GROUP-DOCS] Archivo subido a Cloudinary: "+cloudinaryUrl);

            CommunityDocument doc=new CommunityDocument();
            doc.setLearningCommunity(communityOpt.get());
            doc.setTitle(originalFilename);
            doc.setResourceUrl(cloudinaryUrl);

            CommunityDocument saved=documentRepository.save(doc);

            System.out.println("[GROUP-DOCS] Documento guardado en BD con ID: "+saved.getId());

            Map<String,Object> response=new HashMap<>();
            response.put("success",true);
            response.put("message","Documento subido exitosamente");
            response.put("data",formatDocument(saved));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        }catch(Exception e){
            System.err.println("[GROUP-DOCS] Error upload: "+e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error al subir documento: "+e.getMessage()));
        }
    }

    //#endregion

    //#region Download & View Endpoints

    /**
     * GET /api/group-documents/{documentId}/download
     * Descarga el documento con el nombre original del archivo
     */
    @GetMapping("/{documentId}/download")
    public ResponseEntity<? > downloadDocument(@PathVariable Integer documentId){
        try {
            System.out.println("[GROUP-DOCS] GET /api/group-documents/"+documentId+"/download");

            Optional<CommunityDocument> documentOpt=documentRepository.findById(documentId);
            if(documentOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Documento no encontrado"));
            }

            CommunityDocument document=documentOpt.get();
            String cloudinaryUrl=document.getResourceUrl();
            String originalFilename=document.getTitle();

            if(cloudinaryUrl==null || cloudinaryUrl.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("URL del documento no disponible"));
            }

            System.out.println("[GROUP-DOCS] Descargando desde Cloudinary: "+cloudinaryUrl);
            System.out.println("[GROUP-DOCS] Nombre original: "+originalFilename);

            URL url=new URL(cloudinaryUrl);
            HttpURLConnection connection=(HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if(connection.getResponseCode()!=200){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Error al obtener el documento de Cloudinary"));
            }

            InputStream inputStream=connection.getInputStream();
            byte[] fileContent=inputStream.readAllBytes();
            inputStream.close();

            String encodedFilename=URLEncoder.encode(originalFilename,StandardCharsets.UTF_8).replace("+","%20");

            HttpHeaders headers=new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\""+originalFilename+"\"; filename*=UTF-8''"+encodedFilename);
            headers.setContentLength(fileContent.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            System.out.println("[GROUP-DOCS] Descarga completada: "+fileContent.length+" bytes");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ByteArrayResource(fileContent));

        }catch(Exception e){
            System.err.println("[GROUP-DOCS] Error download: "+e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error al descargar documento: "+e.getMessage()));
        }
    }

    /**
     * GET /api/group-documents/{documentId}/view
     * Visualiza el documento en el navegador (inline)
     */
    @GetMapping("/{documentId}/view")
    public ResponseEntity<? > viewDocument(@PathVariable Integer documentId){
        try {
            System.out.println("[GROUP-DOCS] GET /api/group-documents/"+documentId+"/view");

            Optional<CommunityDocument> documentOpt=documentRepository.findById(documentId);
            if(documentOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Documento no encontrado"));
            }

            CommunityDocument document=documentOpt.get();
            String cloudinaryUrl=document.getResourceUrl();
            String originalFilename=document.getTitle();

            if(cloudinaryUrl==null || cloudinaryUrl.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("URL del documento no disponible"));
            }

            System.out.println("[GROUP-DOCS] Obteniendo documento desde Cloudinary: "+cloudinaryUrl);

            URL url=new URL(cloudinaryUrl);
            HttpURLConnection connection=(HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if(connection.getResponseCode()!=200){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Error al obtener el documento de Cloudinary"));
            }

            InputStream inputStream=connection.getInputStream();
            byte[] fileContent=inputStream.readAllBytes();
            inputStream.close();

            String encodedFilename=URLEncoder.encode(originalFilename,StandardCharsets.UTF_8).replace("+","%20");

            HttpHeaders headers=new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\""+originalFilename+"\"; filename*=UTF-8''"+encodedFilename);
            headers.setContentLength(fileContent.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            System.out.println("[GROUP-DOCS] Vista preparada: "+fileContent.length+" bytes");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ByteArrayResource(fileContent));

        }catch(Exception e){
            System.err.println("[GROUP-DOCS] Error view: "+e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error al visualizar documento: "+e.getMessage()));
        }
    }

    //#endregion

    //#region Delete Endpoint

    /**
     * DELETE /api/group-documents/{documentId}
     * Marca un documento como eliminado (borrado lógico)
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Map<String,Object>> deleteDocument(
            @PathVariable Integer documentId,
            @RequestBody Map<String,String> requestBody,
            @AuthenticationPrincipal UserDetails userDetails){
        try {
            System.out.println("[GROUP-DOCS] DELETE /api/group-documents/"+documentId);

            if(userDetails==null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Usuario no autenticado"));
            }

            String userEmail=userDetails.getUsername();
            Optional<Person> personOpt=personRepository.findByEmail(userEmail);
            if(personOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Usuario no encontrado"));
            }

            Person deletingPerson=personOpt.get();
            String reason=requestBody.get("reason");

            if(reason==null || reason.trim().isEmpty()){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Debe proporcionar una razón para eliminar el documento"));
            }

            Optional<CommunityDocument> documentOpt=documentRepository.findActiveById(documentId);
            if(documentOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Documento no encontrado o ya eliminado"));
            }

            CommunityDocument document=documentOpt.get();

            document.setIsDeleted(true);
            document.setDeletedBy(deletingPerson);
            document.setDeletedAt(new Date());
            document.setDeletionReason(reason.trim());

            documentRepository.save(document);

            System.out.println("[GROUP-DOCS] Documento marcado como eliminado por: "+deletingPerson.getFullName());
            System.out.println("[GROUP-DOCS] Razón: "+reason);

            Map<String,Object> response=new HashMap<>();
            response.put("success",true);
            response.put("message","Documento eliminado exitosamente");

            return ResponseEntity.ok(response);

        }catch(Exception e){
            System.err.println("[GROUP-DOCS] Error delete: "+e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error al eliminar documento: "+e.getMessage()));
        }
    }

    //#endregion

    //#region Storage Endpoints

    /**
     * GET /api/group-documents/community/{communityId}/storage
     */
    @GetMapping("/community/{communityId}/storage")
    public ResponseEntity<Map<String,Object>> getCommunityStorage(
            @PathVariable Long communityId){
        try {
            System.out.println("[GROUP-DOCS] GET /api/group-documents/community/"+communityId+"/storage");

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if(communityOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            List<CommunityDocument> documents=documentRepository.findByCommunityId(communityId);

            long totalSize=0;
            for(CommunityDocument doc:documents){
                totalSize+=1024*50;
            }

            long maxStorage=100*1024*1024;
            long availableStorage=maxStorage-totalSize;
            double usagePercentage=(totalSize*100.0)/maxStorage;

            Map<String,Object> response=new HashMap<>();
            response.put("success",true);
            response.put("data",Map.of(
                    "usedBytes",totalSize,
                    "availableBytes",availableStorage,
                    "maxBytes",maxStorage,
                    "usedFormatted",formatBytes(totalSize),
                    "availableFormatted",formatBytes(availableStorage),
                    "maxFormatted",formatBytes(maxStorage),
                    "usagePercentage",usagePercentage,
                    "documentCount",documents.size()
            ));

            return ResponseEntity.ok(response);

        }catch(Exception e){
            System.err.println("[GROUP-DOCS] Error: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: "+e.getMessage()));
        }
    }

    /**
     * GET /api/group-documents/community/{communityId}/by-date
     */
    @GetMapping("/community/{communityId}/by-date")
    public ResponseEntity<Map<String,Object>> getCommunityDocumentsByDate(
            @PathVariable Long communityId){
        try {
            System.out.println("[GROUP-DOCS] GET /api/group-documents/community/"+communityId+"/by-date");

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if(communityOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            List<CommunityDocument> documents=documentRepository.findByCommunityId(communityId);

            Map<String,List<Map<String,Object>>> groupedByDate=new LinkedHashMap<>();
            for(CommunityDocument doc:documents){
                String dateKey=doc.getUploadDate()!=null ?
                        new java.text.SimpleDateFormat("yyyy-MM-dd").format(doc.getUploadDate()) :
                        "Sin fecha";

                groupedByDate.computeIfAbsent(dateKey,k->new ArrayList<>())
                        .add(formatDocument(doc));
            }

            Map<String,Object> response=new HashMap<>();
            response.put("success",true);
            response.put("data",groupedByDate);

            return ResponseEntity.ok(response);

        }catch(Exception e){
            System.err.println("[GROUP-DOCS] Error: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: "+e.getMessage()));
        }
    }

    /**
     * GET /api/group-documents/community/{communityId}/by-session
     */
    @GetMapping("/community/{communityId}/by-session")
    public ResponseEntity<Map<String,Object>> getCommunityDocumentsBySession(
            @PathVariable Long communityId){
        try {
            System.out.println("[GROUP-DOCS] GET /api/group-documents/community/"+communityId+"/by-session");

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if(communityOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            List<CommunityDocument> documents=documentRepository.findByCommunityId(communityId);

            Map<String,List<Map<String,Object>>> groupedBySession=new LinkedHashMap<>();
            for(CommunityDocument doc:documents){
                String sessionKey=doc.getLearningSession()!=null ?
                        doc.getLearningSession().getTitle() :
                        "Material de Apoyo";

                groupedBySession.computeIfAbsent(sessionKey,k->new ArrayList<>())
                        .add(formatDocument(doc));
            }

            Map<String,Object> response=new HashMap<>();
            response.put("success",true);
            response.put("data",groupedBySession);

            return ResponseEntity.ok(response);

        }catch(Exception e){
            System.err.println("[GROUP-DOCS] Error: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: "+e.getMessage()));
        }
    }

    /**
     * GET /api/group-documents/community/{communityId}
     */
    @GetMapping("/community/{communityId}")
    public ResponseEntity<Map<String,Object>> getDocumentsByCommunity(
            @PathVariable Long communityId){
        try {
            System.out.println("[GROUP-DOCS] GET /api/group-documents/community/"+communityId);

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if(communityOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            List<CommunityDocument> documents=documentRepository.findByCommunityId(communityId);
            List<Map<String,Object>> formattedDocs=new ArrayList<>();

            for(CommunityDocument doc:documents){
                formattedDocs.add(formatDocument(doc));
            }

            Map<String,Object> response=new HashMap<>();
            response.put("success",true);
            response.put("data",formattedDocs);
            response.put("count",formattedDocs.size());

            return ResponseEntity.ok(response);

        }catch(Exception e){
            System.err.println("[GROUP-DOCS] Error: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: "+e.getMessage()));
        }
    }

    /**
     * GET /api/group-documents/community/{communityId}/deleted
     */
    @GetMapping("/community/{communityId}/deleted")
    public ResponseEntity<Map<String,Object>> getDeletedDocuments(
            @PathVariable Long communityId){
        try {
            System.out.println("[GROUP-DOCS] GET /api/group-documents/community/"+communityId+"/deleted");

            Optional<LearningCommunity> communityOpt=communityRepository.findById(communityId);
            if(communityOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Comunidad no encontrada"));
            }

            List<CommunityDocument> deletedDocuments=documentRepository.findDeletedByCommunityId(communityId);
            List<Map<String,Object>> formattedDocs=new ArrayList<>();

            for(CommunityDocument doc:deletedDocuments){
                formattedDocs.add(formatDocument(doc));
            }

            Map<String,Object> response=new HashMap<>();
            response.put("success",true);
            response.put("data",formattedDocs);
            response.put("count",formattedDocs.size());

            return ResponseEntity.ok(response);

        }catch(Exception e){
            System.err.println("[GROUP-DOCS] Error: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: "+e.getMessage()));
        }
    }

    /**
     * GET /api/group-documents/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String,Object>> healthCheck(){
        Map<String,Object> response=new HashMap<>();
        response.put("status","ok");
        response.put("service","group-documents");
        response.put("timestamp",java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    //#endregion

    //#region Private Methods

    /**
     * Formatea un documento para respuesta
     */
    private Map<String,Object> formatDocument(CommunityDocument doc){
        Map<String,Object> map=new HashMap<>();
        map.put("id",doc.getId());
        map.put("title",doc.getTitle());
        map.put("originalFileName",doc.getTitle());
        map.put("resourceUrl",doc.getResourceUrl());
        map.put("uploadDate",doc.getUploadDate());
        map.put("sessionDate",doc.getUploadDate());
        map.put("fileSize",1024*50);
        map.put("formattedFileSize","50 KB");
        map.put("description","");
        map.put("isDeleted",doc.getIsDeleted());

        if(doc.getLearningSession()!=null){
            Map<String,Object> session=new HashMap<>();
            session.put("id",doc.getLearningSession().getId());
            session.put("title",doc.getLearningSession().getTitle());
            map.put("learningSession",session);
        }else{
            map.put("learningSession",null);
        }

        if(doc.getUploadedBy()!=null){
            Map<String,Object> uploader=new HashMap<>();
            uploader.put("id",doc.getUploadedBy().getId());
            uploader.put("fullName",doc.getUploadedBy().getPerson().getFullName());
            uploader.put("profilePhotoUrl",doc.getUploadedBy().getPerson().getProfilePhotoUrl());
            map.put("uploadedBy",uploader);
        }

        if(doc.getIsDeleted() && doc.getDeletedBy()!=null){
            Map<String,Object> deletedByData=new HashMap<>();
            deletedByData.put("id",doc.getDeletedBy().getId());
            deletedByData.put("fullName",doc.getDeletedBy().getFullName());
            deletedByData.put("profilePhotoUrl",doc.getDeletedBy().getProfilePhotoUrl());
            map.put("deletedBy",deletedByData);
            map.put("deletedAt",doc.getDeletedAt());
            map.put("deletionReason",doc.getDeletionReason());
        }

        return map;
    }

    /**
     * Formatea bytes a formato legible
     */
    private String formatBytes(long bytes){
        if(bytes<1024) return bytes+" B";
        int z=(63-Long.numberOfLeadingZeros(bytes))/10;
        return String.format("%.1f %sB",(double)bytes/(1L<<(z*10))," KMGTPE".charAt(z));
    }

    /**
     * Crea respuesta de error
     */
    private Map<String,Object> createErrorResponse(String message){
        Map<String,Object> response=new HashMap<>();
        response.put("success",false);
        response.put("message",message);
        return response;
    }

    //#endregion
}