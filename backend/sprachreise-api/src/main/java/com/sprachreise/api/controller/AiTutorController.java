package com.sprachreise.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiTutorController {

    @Value("${anthropic.api.key:}")
    private String apiKey;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static final String CLAUDE_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL      = "claude-haiku-4-5-20251001";

    private static final String SYSTEM_PROMPT =
        "Tu es un tuteur d'allemand expert et bienveillant nommé 'Max', qui enseigne à des apprenants " +
        "francophones d'Afrique subsaharienne. Tu t'adaptes au niveau CECRL de l'étudiant (A1 à C2). " +
        "Règles strictes :\n" +
        "- Réponds TOUJOURS en français, sauf pour les exemples en allemand\n" +
        "- Sois concis et pédagogique (max 250 mots)\n" +
        "- Donne systématiquement un exemple en allemand suivi de sa traduction\n" +
        "- Pour les fautes, explique la règle puis donne la forme correcte\n" +
        "- Adapte ton vocabulaire au niveau : phrases simples pour A1/A2, plus complexes pour B2/C1/C2\n" +
        "- Si on te pose une question hors allemand, ramène doucement vers l'apprentissage";

    // ── /api/ai/explain ─────────────────────────────────────────────────────
    @PostMapping("/explain")
    public ResponseEntity<?> explain(@RequestBody Map<String, String> body) {
        if (!isConfigured()) return unavailable();
        String level    = body.getOrDefault("level", "A1");
        String question = body.getOrDefault("question", "");
        String context  = body.getOrDefault("context", "");
        String mode     = body.getOrDefault("mode", "explain");
        if (question.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Question vide"));

        String reply = callClaude(SYSTEM_PROMPT, List.of(
            Map.of("role", "user", "content", buildUserMessage(level, question, context, mode))
        ), 400);
        if (reply == null) return serverError();
        return ResponseEntity.ok(Map.of("explanation", reply));
    }

    // ── /api/ai/chat ─────────────────────────────────────────────────────────
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> body) {
        if (!isConfigured()) return unavailable();
        String level     = (String) body.getOrDefault("level", "A1");
        String context   = (String) body.getOrDefault("context", "");
        String firstName = (String) body.getOrDefault("firstName", "");
        String city      = (String) body.getOrDefault("city", "");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.getOrDefault("messages", List.of());
        if (history.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Messages vides"));

        // System prompt personnalisé
        StringBuilder sys = new StringBuilder(SYSTEM_PROMPT);
        if (!firstName.isBlank()) {
            sys.append("\n\nL'étudiant s'appelle ").append(firstName).append(".");
            if (!city.isBlank()) sys.append(" Il/elle vient de ").append(city).append(".");
            sys.append(" Son niveau actuel est ").append(level)
               .append(". Adapte 80% de tes réponses à ce niveau spécifique et utilise son prénom de temps en temps.");
        }

        // Injecter le contexte dans le premier message si présent
        List<Map<String, String>> messages = new ArrayList<>(history);
        if (!context.isBlank() && messages.size() == 1) {
            String enriched = "[Contexte cours : " + context + "]\n\n" + messages.get(0).get("content");
            messages.set(0, Map.of("role", "user", "content", enriched));
        }

        String reply = callClaude(sys.toString(), messages, 500);
        if (reply == null) return serverError();
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    // ── /api/ai/generate-qcm ─────────────────────────────────────────────────
    @PostMapping("/generate-qcm")
    public ResponseEntity<?> generateQcm(@RequestBody Map<String, String> body) {
        if (!isConfigured()) return unavailable();
        String level  = body.getOrDefault("level", "A1");
        String theme  = body.getOrDefault("theme", "vocabulaire général");
        String count  = body.getOrDefault("count", "5");

        String prompt = "Génère exactement " + count + " questions QCM d'allemand pour le niveau " + level +
            " sur le thème : " + theme + ".\n\n" +
            "Format STRICT (JSON valide) :\n" +
            "[\n  {\n    \"question\": \"...\",\n    \"choices\": [\"A) ...\", \"B) ...\", \"C) ...\", \"D) ...\"],\n" +
            "    \"correct\": 0,\n    \"explanation\": \"...\"\n  }\n]\n\n" +
            "- L'index 'correct' est 0-based\n- L'explication est en français\n" +
            "Réponds UNIQUEMENT avec le JSON, sans texte autour.";

        String reply = callClaude(SYSTEM_PROMPT,
            List.of(Map.of("role", "user", "content", prompt)), 1000);
        if (reply == null) return serverError();
        return ResponseEntity.ok(Map.of("qcm", reply));
    }

    // ── Appel HTTP natif Java 11+ ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private String callClaude(String systemPrompt, List<Map<String, String>> messages, int maxTokens) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", MODEL);
            payload.put("max_tokens", maxTokens);
            payload.put("system", systemPrompt);
            payload.put("messages", messages);

            String jsonBody = mapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CLAUDE_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                System.err.println("[AiTutor] Claude returned " + response.statusCode() + ": " + response.body());
                return null;
            }

            Map<String, Object> rb = mapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) rb.get("content");
            if (content != null && !content.isEmpty()) {
                return (String) content.get(0).get("text");
            }
        } catch (Exception e) {
            System.err.println("[AiTutor] Exception: " + e.getClass().getSimpleName() + " — " + e.getMessage());
        }
        return null;
    }

    private String buildUserMessage(String level, String question, String context, String mode) {
        String lvl = "[Niveau " + level + "] ";
        return switch (mode) {
            case "explain"   -> lvl + "Explique ce concept ou règle de grammaire : " + question +
                                (context.isBlank() ? "" : "\nContexte : " + context);
            case "correct"   -> lvl + "Corrige cette phrase et explique l'erreur : \"" + question + "\"" +
                                (context.isBlank() ? "" : "\nQuestion posée : " + context);
            case "translate" -> lvl + "Traduis et explique : \"" + question + "\"";
            case "practice"  -> lvl + "Crée 2 phrases d'exemple avec traduction pour illustrer : " + question;
            case "pronounce" -> lvl + "Conseil de prononciation : " + question +
                                (context.isBlank() ? "" : "\n" + context);
            default          -> lvl + question;
        };
    }

    private boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    private ResponseEntity<?> unavailable() {
        return ResponseEntity.status(503).body(Map.of("error", "Tuteur IA non configuré"));
    }

    private ResponseEntity<?> serverError() {
        return ResponseEntity.status(500).body(Map.of("error", "Erreur lors de la communication avec l'IA"));
    }
}
