package com.project.skillswap.rest.person;

import com.project.skillswap.config.CloudinaryService;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.http.GlobalResponseHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller para gestionar el perfil de Person autenticado
 * Proporciona endpoints para consultar y actualizar información del usuario
 *
 * @author SkillSwap Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/persons")
@CrossOrigin(origins = "*")
public class PersonProfileRestController {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    /**
     * Obtiene el perfil completo del usuario autenticado
     * Endpoint: GET /persons/me
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAuthenticatedPerson(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            Optional<Person> fullPerson = personRepository.findById(authenticatedPerson.getId());

            if (fullPerson.isPresent()) {
                Person person = fullPerson.get();
                
                if (person.getUserSkills() != null) {
                    person.getUserSkills().size();
                }

                return new GlobalResponseHandler().handleResponse(
                        "User profile retrieved successfully",
                        person,
                        HttpStatus.OK,
                        request
                );
            } else {
                return new GlobalResponseHandler().handleResponse(
                        "User profile not found",
                        HttpStatus.NOT_FOUND,
                        request
                );
            }
        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid authentication type");
            errorResponse.put("message", "El usuario autenticado no es del tipo esperado");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            System.err.println("Error getting user profile: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error retrieving profile");
            errorResponse.put("message", "Error al obtener el perfil: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Actualiza el idioma preferido del usuario autenticado
     * Endpoint: PUT /persons/me/language
     *
     * @param request HttpServletRequest
     * @param languageRequest Request body con el nuevo idioma
     * @return ResponseEntity con el perfil actualizado
     */
    @PutMapping("/me/language")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePreferredLanguage(
            HttpServletRequest request,
            @RequestBody Map<String, String> languageRequest
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
            String newLanguage = languageRequest.get("language");

            if (newLanguage == null || newLanguage.trim().isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid language");
                errorResponse.put("message", "Language cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (!isValidLanguageCode(newLanguage)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid language code");
                errorResponse.put("message", "Language code must be 'es' or 'en'");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            person.setPreferredLanguage(newLanguage);
            personRepository.save(person);

            System.out.println(" Language updated for user " + person.getId() + ": " + newLanguage);

            return new GlobalResponseHandler().handleResponse(
                    "Language updated successfully",
                    person,
                    HttpStatus.OK,
                    request
            );

        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid authentication type");
            errorResponse.put("message", "El usuario autenticado no es del tipo esperado");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            System.err.println("Error updating language: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error updating language");
            errorResponse.put("message", "Error al actualizar el idioma: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Actualiza la foto de perfil del usuario autenticado
     * Endpoint: PUT /persons/me/profile-photo
     *
     * @param request HttpServletRequest
     * @param file Archivo de imagen a subir
     * @return ResponseEntity con el perfil actualizado
     */
    @PutMapping("/me/profile-photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfilePhoto(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            System.out.println("[PersonProfileController] Updating profile photo for user: " + authenticatedPerson.getId());

            if (file.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid file");
                errorResponse.put("message", "File cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid file type");
                errorResponse.put("message", "Only image files are allowed (jpg, png, gif, webp)");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            long maxSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxSize) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "File too large");
                errorResponse.put("message", "File size exceeds 5MB limit");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Optional<Person> personOptional = personRepository.findById(authenticatedPerson.getId());
            if (personOptional.isEmpty()) {
                return new GlobalResponseHandler().handleResponse(
                        "User not found",
                        HttpStatus.NOT_FOUND,
                        request
                );
            }

            Person person = personOptional.get();

            if (person.getProfilePhotoUrl() != null && !person.getProfilePhotoUrl().isEmpty()) {
                try {
                    String oldPublicId = cloudinaryService.extractPublicIdFromUrl(person.getProfilePhotoUrl());
                    if (oldPublicId != null) {
                        cloudinaryService.deleteImage(oldPublicId);
                    }
                } catch (Exception e) {
                    System.err.println(" Warning: Could not delete old profile photo: " + e.getMessage());
                }
            }

            String newImageUrl = cloudinaryService.uploadImage(file);

            person.setProfilePhotoUrl(newImageUrl);
            personRepository.save(person);

            System.out.println(" Profile photo updated successfully for user " + person.getId());

            return new GlobalResponseHandler().handleResponse(
                    "Profile photo updated successfully",
                    person,
                    HttpStatus.OK,
                    request
            );

        } catch (IOException e) {
            System.err.println(" Error uploading image to Cloudinary: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Upload failed");
            errorResponse.put("message", "Error uploading image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (ClassCastException e) {
            System.err.println(" Error: Authentication principal is not a Person: " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid authentication type");
            errorResponse.put("message", "El usuario autenticado no es del tipo esperado");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            System.err.println(" Error updating profile photo: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error updating profile photo");
            errorResponse.put("message", "Error al actualizar foto de perfil: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    /**
     * Elimina la foto de perfil del usuario autenticado
     * Endpoint: DELETE /persons/me/profile-photo
     *
     * @param request HttpServletRequest
     * @return ResponseEntity con el perfil actualizado
     */
    @DeleteMapping("/me/profile-photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteProfilePhoto(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            System.out.println("[PersonProfileController] Deleting profile photo for user: " + authenticatedPerson.getId());

            Optional<Person> personOptional = personRepository.findById(authenticatedPerson.getId());
            if (personOptional.isEmpty()) {
                return new GlobalResponseHandler().handleResponse(
                        "User not found",
                        HttpStatus.NOT_FOUND,
                        request
                );
            }

            Person person = personOptional.get();

            if (person.getProfilePhotoUrl() == null || person.getProfilePhotoUrl().isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "No profile photo");
                errorResponse.put("message", "User does not have a profile photo to delete");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            try {
                String publicId = cloudinaryService.extractPublicIdFromUrl(person.getProfilePhotoUrl());
                if (publicId != null) {
                    cloudinaryService.deleteImage(publicId);
                }
            } catch (Exception e) {
                System.err.println(" Warning: Could not delete photo from Cloudinary: " + e.getMessage());
            }

            person.setProfilePhotoUrl(null);
            personRepository.save(person);

            System.out.println(" Profile photo deleted successfully for user " + person.getId());

            return new GlobalResponseHandler().handleResponse(
                    "Profile photo deleted successfully",
                    person,
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
            System.err.println(" Error deleting profile photo: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error deleting profile photo");
            errorResponse.put("message", "Error al eliminar foto de perfil: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     * Endpoint: GET /persons/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "PersonProfile");
        response.put("message", "Person Profile Controller is running");
        return ResponseEntity.ok(response);
    }

    /**
     * Valida si el código de idioma es válido
     * @param languageCode Código del idioma (ej: "es", "en")
     * @return true si es válido, false en caso contrario
     */
    private boolean isValidLanguageCode(String languageCode) {
        return languageCode.equals("es") || languageCode.equals("en");
    }
}