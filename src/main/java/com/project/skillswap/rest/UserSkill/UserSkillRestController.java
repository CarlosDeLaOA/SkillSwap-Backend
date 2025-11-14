package com.project.skillswap.rest.UserSkill;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import com.project.skillswap.logic.entity.UserSkill.UserSkill;
import com.project.skillswap.logic.entity.UserSkill.UserSkillRepository;
import com.project.skillswap.logic.entity.http.GlobalResponseHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping("/user-skills")
@CrossOrigin(origins = "*")
public class UserSkillRestController {

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private PersonRepository personRepository;


    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserSkills(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            System.out.println(" [UserSkillController] Getting skills for user: " + authenticatedPerson.getId());

            // Obtener todas las skills activas del usuario
            List<UserSkill> userSkills = userSkillRepository
                    .findActiveUserSkillsByPersonId(authenticatedPerson.getId());

            System.out.println(" [UserSkillController] Found " + userSkills.size() + " active skills");

            return new GlobalResponseHandler().handleResponse(
                    "User skills retrieved successfully",
                    userSkills,
                    HttpStatus.OK,
                    request
            );

        } catch (ClassCastException e) {
            System.err.println(" Error: Authentication principal is not a Person: " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid authentication type");
            errorResponse.put("message", "El usuario autenticado no es del tipo esperado");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            System.err.println(" Error getting user skills: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error retrieving skills");
            errorResponse.put("message", "Error al obtener habilidades: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{userSkillId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserSkillById(
            HttpServletRequest request,
            @PathVariable Long userSkillId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            Optional<UserSkill> userSkillOptional = userSkillRepository.findById(userSkillId);

            if (userSkillOptional.isEmpty()) {
                return new GlobalResponseHandler().handleResponse(
                        "UserSkill not found",
                        HttpStatus.NOT_FOUND,
                        request
                );
            }

            UserSkill userSkill = userSkillOptional.get();


            if (!userSkill.getPerson().getId().equals(authenticatedPerson.getId())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Forbidden");
                errorResponse.put("message", "No tienes permiso para acceder a esta habilidad");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            System.out.println(" [UserSkillController] Retrieved skill " + userSkillId);

            return new GlobalResponseHandler().handleResponse(
                    "User skill retrieved successfully",
                    userSkill,
                    HttpStatus.OK,
                    request
            );

        } catch (Exception e) {
            System.err.println(" Error getting user skill by ID: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error retrieving skill");
            errorResponse.put("message", "Error al obtener habilidad: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addUserSkills(
            HttpServletRequest request,
            @RequestBody Map<String, Object> skillRequest
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            Optional<Person> personOptional = personRepository.findById(authenticatedPerson.getId());

            if (personOptional.isEmpty()) {
                return new GlobalResponseHandler().handleResponse(
                        "User not found",
                        HttpStatus.NOT_FOUND,
                        request
                );
            }

            Person person = personOptional.get();

            @SuppressWarnings("unchecked")
            List<Integer> skillIdsInt = (List<Integer>) skillRequest.get("skillIds");

            if (skillIdsInt == null || skillIdsInt.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid request");
                errorResponse.put("message", "skillIds cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }


            List<Long> skillIds = skillIdsInt.stream()
                    .map(Integer::longValue)
                    .toList();


            List<Skill> skills = skillRepository.findAllByIdIn(skillIds);

            if (skills.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Skills not found");
                errorResponse.put("message", "No valid skills found with the provided IDs");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            List<UserSkill> addedUserSkills = new ArrayList<>();


            for (Skill skill : skills) {

                Optional<UserSkill> existingUserSkill = userSkillRepository
                        .findByPersonAndSkillId(person, skill.getId());

                if (existingUserSkill.isPresent()) {

                    UserSkill userSkill = existingUserSkill.get();
                    if (!userSkill.getActive()) {
                        userSkill.setActive(true);
                        userSkillRepository.save(userSkill);
                        addedUserSkills.add(userSkill);
                    }
                } else {

                    UserSkill newUserSkill = new UserSkill();
                    newUserSkill.setPerson(person);
                    newUserSkill.setSkill(skill);
                    newUserSkill.setActive(true);
                    userSkillRepository.save(newUserSkill);
                    addedUserSkills.add(newUserSkill);
                }
            }

            System.out.println(" Skills added for user " + person.getId() + ": " + addedUserSkills.size());

            return new GlobalResponseHandler().handleResponse(
                    "Skills added successfully",
                    addedUserSkills,
                    HttpStatus.CREATED,
                    request
            );

        } catch (ClassCastException e) {
            System.err.println(" Error: Authentication principal is not a Person: " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid authentication type");
            errorResponse.put("message", "El usuario autenticado no es del tipo esperado");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            System.err.println(" Error adding skills: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error adding skills");
            errorResponse.put("message", "Error al agregar habilidades: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{userSkillId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteUserSkill(
            HttpServletRequest request,
            @PathVariable Long userSkillId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            Optional<UserSkill> userSkillOptional = userSkillRepository.findById(userSkillId);

            if (userSkillOptional.isEmpty()) {
                return new GlobalResponseHandler().handleResponse(
                        "UserSkill not found",
                        HttpStatus.NOT_FOUND,
                        request
                );
            }

            UserSkill userSkill = userSkillOptional.get();

            if (!userSkill.getPerson().getId().equals(authenticatedPerson.getId())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Forbidden");
                errorResponse.put("message", "No tienes permiso para eliminar esta habilidad");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            List<UserSkill> activeSkills = userSkillRepository
                    .findActiveUserSkillsByPersonId(authenticatedPerson.getId());

            if (activeSkills.size() <= 1) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Validation error");
                errorResponse.put("message", "Debes tener al menos una habilidad activa");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            userSkill.setActive(false);
            userSkillRepository.save(userSkill);

            System.out.println("✅ Skill removed for user " + authenticatedPerson.getId() + ": UserSkill ID " + userSkillId);

            return new GlobalResponseHandler().handleResponse(
                    "Skill removed successfully",
                    HttpStatus.OK,
                    request
            );

        } catch (ClassCastException e) {
            System.err.println(" Error: Authentication principal is not a Person: " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid authentication type");
            errorResponse.put("message", "El usuario autenticado no es del tipo esperado");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            System.err.println(" Error removing skill: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error removing skill");
            errorResponse.put("message", "Error al eliminar habilidad: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "UserSkills");
        response.put("message", "UserSkill Controller is running");
        return ResponseEntity.ok(response);
    }
}