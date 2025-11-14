package com.project.skillswap.rest.KnowledgeArea;

import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeArea;
import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeAreaService;
import com.project.skillswap.logic.entity.http.GlobalResponseHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Knowledge Area operations
 * Provides endpoints to retrieve knowledge areas (categories)
 */
@RestController
@RequestMapping("/knowledge-areas")
@CrossOrigin(origins = "*")
public class KnowledgeAreaRestController {

    //<editor-fold desc="Dependencies">
    @Autowired
    private KnowledgeAreaService knowledgeAreaService;
    //</editor-fold>

    //<editor-fold desc="GET Endpoints">

    /**
     * GET /knowledge-areas
     * Obtiene todas las áreas de conocimiento activas.
     *
     * Este endpoint es público (no requiere autenticación)
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity con la lista de áreas de conocimiento activas
     */
    @GetMapping
    public ResponseEntity<?> getAllKnowledgeAreas(HttpServletRequest request) {
        try {
            List<KnowledgeArea> knowledgeAreas = knowledgeAreaService.getAllActiveKnowledgeAreas();

            return new GlobalResponseHandler().handleResponse(
                    "Knowledge areas retrieved successfully",
                    knowledgeAreas,
                    HttpStatus.OK,
                    request
            );
        } catch (Exception e) {
            System.err.println("Error getting knowledge areas: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(
                            "Error retrieving knowledge areas",
                            "Error retrieving knowledge areas: " + e.getMessage()
                    ));
        }
    }

    /**
     * Health check endpoint to verify service status.
     * Endpoint: GET /knowledge-areas/health
     * No authentication required.
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Knowledge Areas");
        response.put("message", "Knowledge Area Controller is running");
        return ResponseEntity.ok(response);
    }
    //</editor-fold>

    //<editor-fold desc="Private Helper Methods">

    /**
     * Creates an error response map.
     *
     * @param error Error type
     * @param message Error message
     * @return Map containing error information
     */
    private Map<String, String> createErrorResponse(String error, String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return errorResponse;
    }
    //</editor-fold>
}
