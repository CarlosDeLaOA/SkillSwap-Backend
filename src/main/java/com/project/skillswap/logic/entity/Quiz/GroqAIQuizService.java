package com.project.skillswap.logic.entity.Quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Servicio para integración con Groq AI
 * Genera preguntas y respuestas basadas en transcripciones de sesiones
 */
@Service
public class GroqAIQuizService {

    //#region Properties
    private static final Logger logger = LoggerFactory.getLogger(GroqAIQuizService.class);

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.chat.url}")
    private String groqChatUrl;

    @Value("${groq.chat.model}")
    private String groqModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    //#endregion

    //#region Constructor
    public GroqAIQuizService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    //#endregion

    //#region Public Methods
    /**
     * Genera preguntas y opciones basadas en una transcripción usando Groq AI
     *
     * @param transcriptionText texto de la transcripción
     * @param skillName nombre de la habilidad
     * @return mapa con preguntas y opciones
     * @throws Exception si hay error en la generación
     */
    public Map<String, Object> generateQuestionsAndOptions(String transcriptionText, String skillName) throws Exception {
        logger.info("Generando preguntas con Groq AI para skill: {}", skillName);

        if (groqApiKey == null || groqApiKey.trim().isEmpty()) {
            logger.error("Groq API key no configurada");
            throw new IllegalStateException("Groq API key no configurada. Verifica application.properties");
        }

        if (transcriptionText == null || transcriptionText.trim().length() < 50) {
            logger.warn("Transcripción muy corta o vacía, usando preguntas de ejemplo");
            return generateExampleQuestions(skillName);
        }

        try {
            String prompt = buildPrompt(transcriptionText, skillName);
            String groqResponse = callGroqAPI(prompt);
            Map<String, Object> result = parseGroqResponse(groqResponse);

            logger.info("Preguntas generadas exitosamente con Groq AI");
            return result;

        } catch (Exception e) {
            logger.error("Error al generar preguntas con Groq AI: {}", e.getMessage());
            logger.warn("Usando preguntas de ejemplo como fallback");
            return generateExampleQuestions(skillName);
        }
    }
    //#endregion

    //#region Private Methods
    /**
     * Construye el prompt para Groq AI
     *
     * @param transcriptionText transcripción de la sesión
     * @param skillName nombre de la habilidad
     * @return prompt formateado
     */
    private String buildPrompt(String transcriptionText, String skillName) {
        return String.format("""
            Eres un experto en crear evaluaciones educativas. Basándote en la siguiente transcripción de una sesión sobre "%s", genera un cuestionario de evaluación.
            
            TRANSCRIPCIÓN DE LA SESIÓN:
            %s
            
            INSTRUCCIONES IMPORTANTES:
            1. Lee cuidadosamente toda la transcripción
            2. Identifica los conceptos clave, definiciones, ejemplos y puntos principales
            3. Genera exactamente 10 preguntas que evalúen la comprensión del contenido
            4. Las preguntas deben:
               - Ser claras y específicas sobre el contenido de la sesión
               - Cubrir diferentes partes de la transcripción
               - Evaluar comprensión, no memorización literal
               - Ser formuladas de manera profesional
            5. Para cada pregunta, genera UNA respuesta correcta breve (máximo 8 palabras)
            6. Además de las 10 respuestas correctas, genera 2 respuestas distractoras:
               - Deben ser plausibles pero incorrectas
               - Relacionadas con el tema pero no mencionadas en la transcripción
               - Del mismo estilo y longitud que las correctas
            7. Total de opciones: 12 (10 correctas + 2 distractoras)
            
            FORMATO DE RESPUESTA - RESPONDE SOLO CON ESTE JSON (sin markdown, sin texto adicional):
            {
              "questions": [
                {
                  "number": "1",
                  "text": "¿Pregunta específica sobre el contenido?",
                  "correctAnswer": "Respuesta breve"
                },
                {
                  "number": "2",
                  "text": "¿Segunda pregunta sobre otro aspecto?",
                  "correctAnswer": "Otra respuesta"
                }
                ... continuar hasta 10 preguntas
              ],
              "options": [
                "Respuesta correcta 1",
                "Respuesta correcta 2",
                "Respuesta correcta 3",
                "Respuesta correcta 4",
                "Respuesta correcta 5",
                "Respuesta correcta 6",
                "Respuesta correcta 7",
                "Respuesta correcta 8",
                "Respuesta correcta 9",
                "Respuesta correcta 10",
                "Distractor 1",
                "Distractor 2"
              ]
            }
            
            REGLAS CRÍTICAS:
            - Responde ÚNICAMENTE con el JSON, sin explicaciones antes o después
            - NO uses markdown (```json)
            - NO añadas comentarios
            - Las respuestas deben ser CORTAS (máximo 8 palabras)
            - Cada pregunta debe tener su respuesta correcta en el array de opciones
            - Las 12 opciones deben ser todas diferentes
            """, skillName, transcriptionText);
    }

    /**
     * Llama a la API de Groq
     *
     * @param prompt el prompt a enviar
     * @return respuesta de Groq como String
     * @throws Exception si hay error en la llamada
     */
    private String callGroqAPI(String prompt) throws Exception {
        logger.debug("Llamando a Groq API");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqModel);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 2500);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    groqChatUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Error en llamada a Groq API: " + response.getStatusCode());
            }

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            String content = jsonResponse
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            logger.debug("Respuesta recibida de Groq API");
            return content;

        } catch (Exception e) {
            logger.error("Error en llamada a Groq API: {}", e.getMessage());
            throw new Exception("Error al comunicarse con Groq AI: " + e.getMessage(), e);
        }
    }

    /**
     * Parsea la respuesta de Groq AI
     *
     * @param groqResponse respuesta de Groq
     * @return mapa con preguntas y opciones
     * @throws Exception si hay error al parsear
     */
    private Map<String, Object> parseGroqResponse(String groqResponse) throws Exception {
        logger.debug("Parseando respuesta de Groq AI");

        String cleanedResponse = cleanJsonResponse(groqResponse);

        try {
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

            Map<String, Object> result = new HashMap<>();

            List<Map<String, String>> questions = parseQuestions(jsonNode);
            result.put("questions", questions);

            List<String> options = parseOptions(jsonNode);
            result.put("options", options);

            validateQuestionsAndOptions(questions, options);

            logger.info("Respuesta parseada exitosamente: {} preguntas, {} opciones",
                    questions.size(), options.size());

            return result;

        } catch (Exception e) {
            logger.error("Error al parsear respuesta JSON: {}", e.getMessage());
            logger.debug("Respuesta que falló: {}", cleanedResponse);
            throw new Exception("Error al parsear respuesta de Groq AI: " + e.getMessage(), e);
        }
    }

    /**
     * Limpia la respuesta JSON de markdown y espacios
     *
     * @param response respuesta original
     * @return respuesta limpia
     */
    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    /**
     * Parsea las preguntas del JSON
     *
     * @param jsonNode nodo JSON con la respuesta
     * @return lista de preguntas
     * @throws IllegalArgumentException si el formato es inválido
     */
    private List<Map<String, String>> parseQuestions(JsonNode jsonNode) throws IllegalArgumentException {
        List<Map<String, String>> questions = new ArrayList<>();
        JsonNode questionsNode = jsonNode.get("questions");

        if (questionsNode == null || !questionsNode.isArray()) {
            throw new IllegalArgumentException("La respuesta debe contener un array de preguntas");
        }

        for (JsonNode questionNode : questionsNode) {
            Map<String, String> question = new HashMap<>();
            question.put("number", questionNode.get("number").asText());
            question.put("text", questionNode.get("text").asText());
            question.put("correctAnswer", questionNode.get("correctAnswer").asText());
            questions.add(question);
        }

        return questions;
    }

    /**
     * Parsea las opciones del JSON
     *
     * @param jsonNode nodo JSON con la respuesta
     * @return lista de opciones
     * @throws IllegalArgumentException si el formato es inválido
     */
    private List<String> parseOptions(JsonNode jsonNode) throws IllegalArgumentException {
        List<String> options = new ArrayList<>();
        JsonNode optionsNode = jsonNode.get("options");

        if (optionsNode == null || !optionsNode.isArray()) {
            throw new IllegalArgumentException("La respuesta debe contener un array de opciones");
        }

        for (JsonNode optionNode : optionsNode) {
            options.add(optionNode.asText());
        }

        return options;
    }

    /**
     * Valida que las preguntas y opciones sean correctas
     *
     * @param questions lista de preguntas
     * @param options lista de opciones
     * @throws IllegalArgumentException si la validación falla
     */
    private void validateQuestionsAndOptions(List<Map<String, String>> questions, List<String> options)
            throws IllegalArgumentException {

        if (questions.size() != 10) {
            logger.warn("Se esperaban 10 preguntas pero se recibieron {}", questions.size());
            if (questions.size() < 10) {
                throw new IllegalArgumentException("Se requieren exactamente 10 preguntas, se recibieron " + questions.size());
            }
        }

        if (options.size() != 12) {
            logger.warn("Se esperaban 12 opciones pero se recibieron {}", options.size());
            if (options.size() < 12) {
                throw new IllegalArgumentException("Se requieren exactamente 12 opciones, se recibieron " + options.size());
            }
        }

        for (Map<String, String> question : questions) {
            String correctAnswer = question.get("correctAnswer");
            if (!options.contains(correctAnswer)) {
                logger.error("Respuesta correcta no encontrada en opciones: {}", correctAnswer);
                throw new IllegalArgumentException("La respuesta correcta debe estar en las opciones: " + correctAnswer);
            }
        }

        Set<String> uniqueOptions = new HashSet<>(options);
        if (uniqueOptions.size() != options.size()) {
            logger.warn("Se encontraron opciones duplicadas");
        }
    }

    /**
     * Genera preguntas de ejemplo cuando Groq AI no está disponible
     *
     * @param skillName nombre de la habilidad
     * @return mapa con preguntas y opciones de ejemplo
     */
    private Map<String, Object> generateExampleQuestions(String skillName) {
        logger.info("Generando preguntas de ejemplo para skill: {}", skillName);

        Map<String, Object> result = new HashMap<>();

        List<Map<String, String>> questions = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, String> question = new HashMap<>();
            question.put("number", String.valueOf(i));
            question.put("text", String.format("Pregunta %d sobre %s (generada automáticamente)", i, skillName));
            question.put("correctAnswer", "Respuesta " + i);
            questions.add(question);
        }
        result.put("questions", questions);

        List<String> options = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            options.add("Respuesta " + i);
        }
        Collections.shuffle(options);
        result.put("options", options);

        return result;
    }
    //#endregion
}