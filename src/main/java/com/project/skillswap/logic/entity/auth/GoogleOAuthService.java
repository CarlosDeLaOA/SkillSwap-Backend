package com.project.skillswap.logic.entity.auth;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
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
    //#endregion

    //#region Constructor
    /**
     * Constructor del servicio de Google OAuth.
     *
     * @param restTemplate Cliente REST para llamadas HTTP
     * @param personRepository Repositorio de personas
     */
    public GoogleOAuthService(RestTemplate restTemplate, PersonRepository personRepository) {
        this.restTemplate = restTemplate;
        this.personRepository = personRepository;
        this.objectMapper = new ObjectMapper();
    }
    //#endregion

    //#region OAuth Flow Methods
    /**
     * Intercambia el código de autorización por un access token de Google.
     *
     * @param code Código de autorización de Google
     * @param redirectUri URI de redirección
     * @return Mapa con el access token y datos relacionados
     * @throws RuntimeException si falla la obtención del token
     */
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

    /**
     * Obtiene la información del usuario desde Google.
     *
     * @param accessToken Token de acceso de Google
     * @return Mapa con información del usuario de Google
     * @throws RuntimeException si falla la obtención de información
     */
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
     * Procesa el login o registro del usuario con datos de Google.
     * Si el usuario existe, actualiza su información. Si no existe, lo crea.
     *
     * @param userInfo Mapa con información del usuario de Google
     * @return Usuario creado o actualizado
     * @throws IOException si falla el procesamiento de la imagen
     */
    public Person processGoogleUser(Map<String, Object> userInfo) throws IOException {
        String email = (String) userInfo.get("email");
        Optional<Person> existingPerson = personRepository.findByEmail(email);

        if (existingPerson.isPresent()) {
            return updateExistingUser(existingPerson.get(), userInfo);
        } else {
            return createNewUser(userInfo);
        }
    }

    /**
     * Actualiza un usuario existente con datos de Google.
     *
     * @param person Usuario existente
     * @param userInfo Mapa con información actualizada de Google
     * @return Usuario actualizado
     * @throws IOException si falla el procesamiento de la imagen
     */
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
     * Crea un nuevo usuario con datos de Google.
     *
     * @param userInfo Mapa con información del usuario de Google
     * @return Nuevo usuario creado
     * @throws IOException si falla el procesamiento de la imagen
     */
    private Person createNewUser(Map<String, Object> userInfo) throws IOException {
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

        // CRÍTICO: Establecer passwordHash para usuarios OAuth
        // Los usuarios OAuth no pueden hacer login con contraseña tradicional
        newPerson.setPasswordHash("OAUTH_USER_NO_PASSWORD");

        String picture = (String) userInfo.get("picture");
        if (picture != null) {
            String profileImagePath = downloadAndResizeProfileImage(picture);
            newPerson.setProfilePhotoUrl(profileImagePath);
        }

        return personRepository.save(newPerson);
    }

    /**
     * Descarga y redimensiona la imagen de perfil de Google.
     *
     * @param imageUrl URL de la imagen de perfil de Google
     * @return Ruta local de la imagen procesada
     * @throws IOException si falla la descarga o procesamiento
     */
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

    /**
     * Redimensiona una imagen a las dimensiones especificadas.
     *
     * @param originalImage Imagen original
     * @param targetWidth Ancho objetivo
     * @param targetHeight Alto objetivo
     * @return Imagen redimensionada
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image scaledImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        resizedImage.getGraphics().drawImage(scaledImage, 0, 0, null);

        return resizedImage;
    }

    /**
     * Valida si un usuario necesita completar el proceso de onboarding.
     *
     * @param person Usuario a validar
     * @return true si necesita onboarding, false en caso contrario
     */
    public boolean requiresOnboarding(Person person) {
        return person.getPreferredLanguage() == null || person.getPreferredLanguage().isEmpty();
    }

}