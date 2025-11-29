package com.project.skillswap.rest.notification;

import com.project.skillswap.logic.entity.CommunityDocument.CommunityDocument;
import com.project.skillswap.logic.entity.CommunityDocument.CommunityDocumentRepository;
import com.project.skillswap.logic.entity.CommunityMember.CommunityMember;
import com.project.skillswap.logic.entity.CommunityMember.CommunityMemberRepository;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunityRepository;
import com.project.skillswap.logic.entity.Notification.NotificationService;
import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ⚠️ CONTROLADOR TEMPORAL SOLO PARA PRUEBAS DE NOTIFICACIONES
 * Eliminar cuando se implementen los servicios reales
 */
@RestController
@RequestMapping("/api/notifications/test")
@CrossOrigin(origins = "*")
public class NotificationTestController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CommunityDocumentRepository documentRepository;

    @Autowired
    private LearningCommunityRepository communityRepository;

    @Autowired
    private CommunityMemberRepository memberRepository;

    /**
     * POST /api/notifications/test/document-added/{documentId}
     * Simula la notificación de documento agregado
     */
    @PostMapping("/document-added/{documentId}")
    public ResponseEntity<Map<String, Object>> testDocumentAdded(@PathVariable Integer documentId) {
        try {
            System.out.println("🧪 [TEST] Probando notificación de documento agregado");
            System.out.println("🧪 [TEST] Document ID: " + documentId);

            // Buscar el documento
            CommunityDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Documento no encontrado con ID: " + documentId));

            System.out.println("🧪 [TEST] Documento encontrado: " + document.getTitle());
            System.out.println("🧪 [TEST] Comunidad ID: " + document.getLearningCommunity().getId());

            // Obtener todos los miembros activos de la comunidad
            List<CommunityMember> members = memberRepository
                    .findActiveMembersByCommunityId(document.getLearningCommunity().getId());

            System.out.println("🧪 [TEST] Miembros activos encontrados: " + members.size());

            // Convertir a lista de Person
            List<Person> recipients = members.stream()
                    .map(cm -> {
                        Person p = cm.getLearner().getPerson();
                        System.out.println("🧪 [TEST] Miembro: " + p.getFullName() + " (ID: " + p.getId() + ", Email: " + p.getEmail() + ")");
                        return p;
                    })
                    .collect(Collectors.toList());

            System.out.println("🧪 [TEST] Total recipients para notificar: " + recipients.size());
            System.out.println("📨 [TEST] Llamando a notificationService.notifyDocumentAdded()...");

            // Disparar notificación
            notificationService.notifyDocumentAdded(document, recipients);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notificaciones enviadas exitosamente");
            response.put("recipientsCount", recipients.size());
            response.put("documentTitle", document.getTitle());
            response.put("communityName", document.getLearningCommunity().getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ [TEST] Error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * POST /api/notifications/test/member-joined/{communityId}/{newMemberId}
     * Simula la notificación de nuevo miembro
     */
    @PostMapping("/member-joined/{communityId}/{newMemberId}")
    public ResponseEntity<Map<String, Object>> testMemberJoined(
            @PathVariable Long communityId,
            @PathVariable Long newMemberId) {
        try {
            System.out.println("🧪 [TEST] Probando notificación de nuevo miembro");

            // Buscar comunidad
            LearningCommunity community = communityRepository.findById(communityId)
                    .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

            // Buscar el nuevo miembro
            CommunityMember newMember = memberRepository.findById(newMemberId)
                    .orElseThrow(() -> new RuntimeException("Miembro no encontrado"));

            Person newMemberPerson = newMember.getLearner().getPerson();

            // Obtener todos los miembros activos
            List<CommunityMember> members = memberRepository
                    .findActiveMembersByCommunityId(communityId);

            List<Person> recipients = members.stream()
                    .map(cm -> cm.getLearner().getPerson())
                    .collect(Collectors.toList());

            System.out.println("📨 [TEST] Enviando notificación a " + recipients.size() + " miembros");

            // Disparar notificación
            notificationService.notifyMemberJoined(community, newMemberPerson, recipients);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notificaciones enviadas exitosamente");
            response.put("recipientsCount", recipients.size());
            response.put("newMemberName", newMemberPerson.getFullName());
            response.put("communityName", community.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ [TEST] Error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * POST /api/notifications/test/member-left/{communityId}/{leftMemberId}
     * Simula la notificación de miembro que salió
     */
    @PostMapping("/member-left/{communityId}/{leftMemberId}")
    public ResponseEntity<Map<String, Object>> testMemberLeft(
            @PathVariable Long communityId,
            @PathVariable Long leftMemberId) {
        try {
            System.out.println("🧪 [TEST] Probando notificación de miembro que salió");

            // Buscar comunidad
            LearningCommunity community = communityRepository.findById(communityId)
                    .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

            // Buscar el miembro que salió
            CommunityMember leftMember = memberRepository.findById(leftMemberId)
                    .orElseThrow(() -> new RuntimeException("Miembro no encontrado"));

            Person leftMemberPerson = leftMember.getLearner().getPerson();
            Person creator = community.getCreator().getPerson();

            System.out.println("📨 [TEST] Enviando notificación al creador: " + creator.getFullName());

            // Disparar notificación (solo al creador)
            notificationService.notifyMemberLeft(community, leftMemberPerson, creator);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notificación enviada al creador exitosamente");
            response.put("leftMemberName", leftMemberPerson.getFullName());
            response.put("creatorName", creator.getFullName());
            response.put("communityName", community.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ [TEST] Error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * POST /api/notifications/test/achievement/{communityId}/{achieverId}
     * Simula la notificación de logro obtenido
     */
    @PostMapping("/achievement/{communityId}/{achieverId}")
    public ResponseEntity<Map<String, Object>> testAchievement(
            @PathVariable Long communityId,
            @PathVariable Long achieverId,
            @RequestBody Map<String, String> body) {
        try {
            System.out.println("🧪 [TEST] Probando notificación de logro");

            String achievementName = body.getOrDefault("achievementName", "React Avanzado");

            // Buscar comunidad
            LearningCommunity community = communityRepository.findById(communityId)
                    .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

            // Buscar el miembro que obtuvo el logro
            CommunityMember achiever = memberRepository.findById(achieverId)
                    .orElseThrow(() -> new RuntimeException("Miembro no encontrado"));

            Person achieverPerson = achiever.getLearner().getPerson();

            // Obtener todos los miembros activos
            List<CommunityMember> members = memberRepository
                    .findActiveMembersByCommunityId(communityId);

            List<Person> recipients = members.stream()
                    .map(cm -> cm.getLearner().getPerson())
                    .collect(Collectors.toList());

            System.out.println("📨 [TEST] Enviando notificación a " + recipients.size() + " miembros");

            // Disparar notificación
            notificationService.notifyAchievementEarned(community, achieverPerson, achievementName, recipients);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notificaciones enviadas exitosamente");
            response.put("recipientsCount", recipients.size());
            response.put("achieverName", achieverPerson.getFullName());
            response.put("achievementName", achievementName);
            response.put("communityName", community.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ [TEST] Error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}