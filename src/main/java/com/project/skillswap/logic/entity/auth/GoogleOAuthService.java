package com.project.skillswap.logic.entity.auth;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.ZoneId;
import java.util.Date;
import java.math.BigDecimal;

@Service
public class GoogleOAuthService {
    //#region Properties
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v1/userinfo";
    private static final int PROFILE_IMAGE_SIZE = 200;
    private static final String UPLOAD_DIR = "uploads/profiles/";

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final PersonRepository personRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private LearnerRepository learnerRepository;
    //#endregion

    //#region Constructor
    public GoogleOAuthService(RestTemplate restTemplate, PersonRepository personRepository) {
        this.restTemplate = restTemplate;
        this.personRepository = personRepository;
        this.objectMapper = new ObjectMapper();
    }
    //#endregion

    //#region OAuth Flow Methods
    public Map<String, Object> exchangeCodeForToken(String code, String redirectUri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_TOKEN_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("accessToken", jsonNode.get("access_token").asText());
            tokenData.put("expiresIn", jsonNode.has("expires_in") ? jsonNode.get("expires_in").asInt() : null);
            tokenData.put("tokenType", jsonNode.has("token_type") ? jsonNode.get("token_type").asText() : null);

            return tokenData;
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Error al obtener token de Google: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar respuesta de Google: " + e.getMessage());
        }
    }

    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", jsonNode.get("id").asText());
            userInfo.put("email", jsonNode.get("email").asText());
            userInfo.put("verifiedEmail", jsonNode.has("verified_email") && jsonNode.get("verified_email").asBoolean());
            userInfo.put("name", jsonNode.has("name") ? jsonNode.get("name").asText() : null);
            userInfo.put("givenName", jsonNode.has("given_name") ? jsonNode.get("given_name").asText() : null);
            userInfo.put("familyName", jsonNode.has("family_name") ? jsonNode.get("family_name").asText() : null);
            userInfo.put("picture", jsonNode.has("picture") ? jsonNode.get("picture").asText() : null);

            return userInfo;
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Error al obtener información del usuario de Google: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar información del usuario: " + e.getMessage());
        }
    }
    //#endregion

    //#region User Management Methods
    /**
     * Procesa el login o registro del usuario con datos de Google y rol opcional.
     * Si el usuario existe, actualiza su información. Si no existe, lo crea con el rol especificado.
     *
     * @param userInfo Mapa con información del usuario de Google
     * @param role Rol seleccionado por el usuario ("LEARNER" o "INSTRUCTOR"), puede ser null para usuarios existentes
     * @return Usuario creado o actualizado
     * @throws IOException si falla el procesamiento de la imagen
     */
    public Person processGoogleUser(Map<String, Object> userInfo, String role) throws IOException {
        String email = (String) userInfo.get("email");
        Optional<Person> existingPerson = personRepository.findByEmail(email);

        if (existingPerson.isPresent()) {
            System.out.println("[GOOGLE-OAUTH] Usuario existente encontrado: " + email);
            return updateExistingUser(existingPerson.get(), userInfo);
        } else {
            // Si es un nuevo usuario, el rol es obligatorio
            if (role == null || role.trim().isEmpty()) {
                throw new IllegalArgumentException("El rol es requerido para nuevos usuarios");
            }
            System.out.println("[GOOGLE-OAUTH] Creando nuevo usuario con rol: " + role);
            return createNewUser(userInfo, role);
        }
    }

    private Person updateExistingUser(Person person, Map<String, Object> userInfo) throws IOException {
        String name = (String) userInfo.get("name");
        if (name != null && !name.equals(person.getFullName())) {
            person.setFullName(name);
        }

        String picture = (String) userInfo.get("picture");
        if (picture != null) {
            String profileImagePath = downloadAndResizeProfileImage(picture);
            person.setProfilePhotoUrl(profileImagePath);
        }

        person.setGoogleOauthId((String) userInfo.get("id"));
        person.setLastConnection(
                Date.from(LocalDateTime.now()
                        .atZone(ZoneId.systemDefault())
                        .toInstant())
        );

        return personRepository.save(person);
    }

    /**
     * Crea un nuevo usuario con datos de Google y el rol especificado.
     *
     * @param userInfo Mapa con información del usuario de Google
     * @param role Rol del usuario ("LEARNER" o "INSTRUCTOR")
     * @return Nuevo usuario creado con su rol asignado
     * @throws IOException si falla el procesamiento de la imagen
     */
    private Person createNewUser(Map<String, Object> userInfo, String role) throws IOException {
        System.out.println("[GOOGLE-OAUTH] Iniciando creación de usuario con rol: " + role);

        Person newPerson = new Person();
        newPerson.setEmail((String) userInfo.get("email"));
        newPerson.setFullName((String) userInfo.get("name"));
        newPerson.setGoogleOauthId((String) userInfo.get("id"));
        newPerson.setEmailVerified((Boolean) userInfo.get("verifiedEmail"));
        newPerson.setLastConnection(
                Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
        );
        newPerson.setRegistrationDate(
                Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
        );
        newPerson.setActive(true);
        newPerson.setPasswordHash("OAUTH_USER_NO_PASSWORD");

        String picture = (String) userInfo.get("picture");
        if (picture != null) {
            String profileImagePath = downloadAndResizeProfileImage(picture);
            newPerson.setProfilePhotoUrl(profileImagePath);
        }

        // Guardar Person primero
        Person savedPerson = personRepository.save(newPerson);
        System.out.println("[GOOGLE-OAUTH] Person guardado con ID: " + savedPerson.getId());

        // Crear el rol correspondiente
        if ("INSTRUCTOR".equalsIgnoreCase(role)) {
            createInstructor(savedPerson);
        } else {
            createLearner(savedPerson);
        }

        return savedPerson;
    }

    /**
     * Crea un Instructor para el usuario especificado
     */
    private void createInstructor(Person person) {
        System.out.println("[GOOGLE-OAUTH] Creando Instructor para person ID: " + person.getId());

        Instructor instructor = new Instructor();
        instructor.setPerson(person);
        instructor.setSkillcoinsBalance(BigDecimal.ZERO);
        instructor.setVerifiedAccount(false);
        instructor.setAverageRating(BigDecimal.ZERO);
        instructor.setSessionsTaught(0);
        instructor.setTotalEarnings(BigDecimal.ZERO);

        instructorRepository.save(instructor);
        System.out.println("[GOOGLE-OAUTH] Instructor creado exitosamente");
    }

    /**
     * Crea un Learner para el usuario especificado
     */
    private void createLearner(Person person) {
        System.out.println("[GOOGLE-OAUTH] Creando Learner para person ID: " + person.getId());

        Learner learner = new Learner();
        learner.setPerson(person);
        learner.setSkillcoinsBalance(BigDecimal.ZERO);
        learner.setCompletedSessions(0);
        learner.setCredentialsObtained(0);

        learnerRepository.save(learner);
        System.out.println("[GOOGLE-OAUTH] Learner creado exitosamente");
    }

    private String downloadAndResizeProfileImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        BufferedImage originalImage = ImageIO.read(url);

        if (originalImage == null) {
            throw new IOException("No se pudo leer la imagen de perfil");
        }

        BufferedImage resizedImage = resizeImage(originalImage, PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE);

        String fileName = UUID.randomUUID().toString() + ".jpg";
        Path uploadPath = Paths.get(UPLOAD_DIR);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        ImageIO.write(resizedImage, "jpg", filePath.toFile());

        return UPLOAD_DIR + fileName;
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image scaledImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        resizedImage.getGraphics().drawImage(scaledImage, 0, 0, null);

        return resizedImage;
    }

    public boolean requiresOnboarding(Person person) {
        boolean noLanguage = person.getPreferredLanguage() == null || person.getPreferredLanguage().isEmpty();

        boolean noSkills = (person.getUserSkills() == null || person.getUserSkills().isEmpty());

        return noLanguage || noSkills;
    }

}