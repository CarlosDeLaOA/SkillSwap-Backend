
package com.project.skillswap.logic.entity.videocall;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Servicio encargado de generar resúmenes automáticos de sesiones utilizando
 * modelos de IA a través de la API de Groq.
 *
 * Funcionalidades principales:
 * - Validación del contenido de transcripciones
 * - Construcción de prompts optimizados
 * - Envío de solicitudes al modelo LLM
 * - Validación de calidad del resumen generado
 */
@Service
public class SessionSummaryService {

    //#region Configuración

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.chat.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqChatUrl;

    @Value("${groq.chat.model:llama-3.3-70b-versatile}")
    private String groqChatModel;

    private final OkHttpClient httpClient;
    private final Gson gson;

    //#endregion


    //#region Constructor

    public SessionSummaryService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .build();

        this.gson = new Gson();
    }

    //#endregion

    // GENERACIÓN DE RESUMEN
    /**
     * Genera un resumen conciso basado en la transcripción de una sesión.
     *
     * @param session Sesión con transcripción almacenada
     * @return Resumen generado por IA
     * @throws IOException Si ocurre un error al comunicarse con la API Groq
     */
    public String generateSummary(LearningSession session) throws IOException {

        String transcription = session.getFullText();

        if (transcription == null || transcription.isEmpty()) {
            throw new IllegalArgumentException("La sesión no tiene transcripción");
        }

        // Validación mínima de contenido
        if (!hasMinimumContent(transcription)) {
            return formattedInsufficientContentMessage(transcription);
        }

        // Construcción de prompt para solicitar resumen
        String prompt = buildSummaryPrompt(session, transcription);

        // Llamada a la API externa
        String summary = callGroqChat(prompt);

        // Validación de resumen
        if (summary.toLowerCase().contains("no hay suficiente contenido")) {
            return formattedInsufficientContentMessage(transcription);
        }

        return summary.trim();
    }

    /**
     * Devuelve un mensaje estructurado cuando la transcripción tiene contenido insuficiente.
     */
    private String formattedInsufficientContentMessage(String transcription) {

        int wordCount = transcription.split("\\s+").length;
        int uniqueCount = countUniqueWords(transcription);

        return " **Contenido Insuficiente**\n\n" +
                "Esta sesión no tiene suficiente contenido educativo para generar un resumen significativo.\n\n" +
                "**Detalles:**\n" +
                "- Palabras en transcripción: " + wordCount + "\n" +
                "- Palabras únicas: " + uniqueCount + "\n" +
                "- Se requiere un mínimo aproximado de 50 palabras con variedad.\n\n" +
                "**Recomendación:** Verifica que la grabación haya capturado correctamente el audio.";
    }



    // VALIDACIÓN DE CONTENIDO
    /**
     * Valida que el texto tenga el contenido mínimo necesario para generar un resumen útil.
     * (Validación reducida para maximizar cobertura)
     */
    private boolean hasMinimumContent(String transcription) {

        if (transcription == null || transcription.trim().isEmpty()) {
            return false;
        }

        String[] words = transcription.trim().toLowerCase().split("\\s+");
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));

        int total = words.length;
        int unique = uniqueWords.size();

        return total >= 30 && unique >= 15;
    }

    /**
     * Cuenta palabras únicas de un texto.
     */
    private int countUniqueWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        Set<String> unique = new HashSet<>(Arrays.asList(text.trim().toLowerCase().split("\\s+")));
        return unique.size();
    }



    // CONSTRUCCIÓN DE PROMPTS
    /**
     * Construye el prompt que será enviado al modelo IA para generar un resumen.
     */
    private String buildSummaryPrompt(LearningSession session, String transcription) {

        String title = session.getTitle();
        String skill = session.getSkill() != null ? session.getSkill().getName() : "N/A";

        String instructor =
                session.getInstructor() != null &&
                        session.getInstructor().getPerson() != null
                        ? session.getInstructor().getPerson().getFullName()
                        : "N/A";

        return String.format("""
            Eres un asistente experto en crear resúmenes de sesiones educativas.

            **TU TAREA:** Resume el contenido de esta sesión de forma clara y concisa.

            **INFORMACIÓN DE LA SESIÓN:**
            - Título: %s
            - Habilidad: %s
            - Instructor: %s

            **TRANSCRIPCIÓN COMPLETA:**
            ```
            %s
            ```

            **INSTRUCCIONES:**
            1. Identifica los temas principales discutidos
            2. Crea un resumen estructurado con:
               • Tema Principal  
               • Puntos Clave  
               • Conclusiones  
            3. Escribe en español
            4. Máximo 400 palabras
            5. Usa únicamente información presente en la transcripción
            """,
                title, skill, instructor, transcription
        );
    }


    // CONEXIÓN CON GROQ
    /**
     * Realiza una llamada a la API de Groq usando OkHttp.
     *
     * @param prompt Texto del prompt a enviar
     * @return Resumen generado por la IA
     * @throws IOException Si ocurre un error de red o respuesta inesperada
     */
    private String callGroqChat(String prompt) throws IOException {

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("model", groqChatModel);

        JsonArray messages = new JsonArray();
        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("role", "user");
        messageObj.addProperty("content", prompt);
        messages.add(messageObj);

        requestJson.add("messages", messages);
        requestJson.addProperty("temperature", 0.3);
        requestJson.addProperty("max_tokens", 1500);
        requestJson.addProperty("top_p", 0.9);

        RequestBody body = RequestBody.create(
                requestJson.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(groqChatUrl)
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                String errorPayload = response.body() != null ? response.body().string() : "Sin detalles";
                throw new IOException("Error HTTP " + response.code() + ": " + errorPayload);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            JsonArray choices = jsonResponse.getAsJsonArray("choices");

            if (choices != null && choices.size() > 0) {
                JsonObject msg = choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message");

                return msg.get("content").getAsString();
            }

            throw new IOException("Respuesta inesperada de Groq: " + responseBody);
        }
    }


    // VALIDACIÓN DE RESUMEN
    /**
     * Verifica que el resumen generado sea válido y útil.
     *
     * @param summary Texto del resumen
     * @return true si cumple criterios mínimos
     */
    public boolean validateSummary(String summary) {

        if (summary == null || summary.trim().isEmpty()) {
            return false;
        }

        if (summary.contains("Contenido Insuficiente")) {
            return true;
        }

        int wordCount = summary.split("\\s+").length;

        return wordCount >= 30 && wordCount <= 800;
    }

}
