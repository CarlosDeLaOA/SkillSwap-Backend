package com.project.skillswap.rest.LearningCommunity;

import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunityRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.auth.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller para gestión de comunidades de aprendizaje
 */
@RestController
@RequestMapping("/api/communities")
@CrossOrigin(origins = "http://localhost:4200")
public class LearningCommunityRestController {

    @Autowired
    private LearningCommunityRepository learningCommunityRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private JwtService jwtService;

    /**
     * GET /api/communities/my-communities
     * Obtiene todas las comunidades del usuario autenticado
     */
    @GetMapping("/my-communities")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getMyCommunities(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Integer maxMembers) {
        try {
            System.out.println("[COMMUNITIES] GET /api/communities/my-communities");

            String token = authHeader.replace("Bearer ", "");
            String userEmail = jwtService.extractUsername(token);

            System.out.println("[COMMUNITIES] Usuario autenticado: " + userEmail);

            // Buscar persona por email
            Person person = personRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

            Learner learner = person.getLearner();
            if (learner == null) {
                throw new RuntimeException("El usuario no tiene un perfil de estudiante");
            }

            // Obtener comunidades
            List<LearningCommunity> communities;
            if (maxMembers != null) {
                communities = learningCommunityRepository.findCommunitiesByLearnerIdWithMaxMembers(
                        learner.getId(),
                        maxMembers
                );
                System.out.println("[COMMUNITIES] Comunidades con máximo " + maxMembers + " miembros: " + communities.size());
            } else {
                communities = learningCommunityRepository.findCommunitiesByLearnerId(learner.getId());
                System.out.println("[COMMUNITIES] Total de comunidades: " + communities.size());
            }

            System.out.println("[COMMUNITIES] 🔄 Forzando carga de miembros...");
            communities.forEach(community -> {
                try {
                    List<com.project.skillswap.logic.entity.CommunityMember.CommunityMember> members = community.getMembers();
                    int memberCount = members != null ? members.size() : 0;
                    System.out.println("[COMMUNITIES] ✅ Comunidad '" + community.getName() + "' tiene " + memberCount + " miembros");

                    // Debug adicional
                    if (members != null && !members.isEmpty()) {
                        System.out.println("[COMMUNITIES]    └─ Miembros cargados en memoria");
                    } else {
                        System.out.println("[COMMUNITIES]    └─ ⚠️ Lista de miembros vacía o null");
                    }
                } catch (Exception e) {
                    System.err.println("[COMMUNITIES] ❌ Error al cargar miembros de '" + community.getName() + "': " + e.getMessage());
                    e.printStackTrace();
                }
            });

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", communities);
            response.put("count", communities.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("[COMMUNITIES] Error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}