package com.sprachreise.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiTutorController {

    @Value("${anthropic.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SYSTEM_PROMPT =
        "Tu es un tuteur d'allemand expert et bienveillant nommé 'Max', qui enseigne à des apprenants " +
        "francophones d'Afrique subsaharienne. Tu t'adaptes au niveau CECRL de l'étudiant (A1 à C2). " +
        "Règles strictes :\n" +
        "- Réponds TOUJOURS en français, sauf pour les exemples en allemand\n" +
        "- Sois concis et pédagogique (max 250 mots par réponse)\n" +
        "- Donne systématiquement un exemple en allemand suivi de sa traduction\n" +
        "- Pour les fautes, explique la règle puis donne la forme correcte\n" +
        "- Utilise des emojis pédagogiques 📚✅❌💡 pour rendre la lecture plus facile\n" +
        "- Adapte ton vocabulaire au niveau : phrases simples pour A1/A2, plus complexes pour B2/C1/C2\n" +
        "- Si on te pose une question hors allemand, ramène doucement vers l'apprentissage";

    // ─── Endpoint principal : explication / correction / traduction ─────────
    @PostMapping("/explain")
    public ResponseEntity<?> explain(@RequestBody Map<String, String> body) {
        if (!isConfigured()) return unavailable();

        String level    = body.getOrDefault("level", "A1");
        String question = body.getOrDefault("question", "");
        String context  = body.getOrDefault("context", "");
        String mode     = body.getOrDefault("mode", "explain");

        if (question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question vide"));
        }

        String userMessage = buildUserMessage(level, question, context, mode);
        String reply = callClaude(List.of(Map.of("role", "user", "content", userMessage)), 400);

        if (reply == null) return serverError();
        return ResponseEntity.ok(Map.of("explanation", reply));
    }

    // ─── Endpoint chat : conversation avec historique ───────────────────────
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> body) {
        if (!isConfigured()) return unavailable();

        String level = (String) body.getOrDefault("level", "A1");
        String context = (String) body.getOrDefault("context", "");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.getOrDefault("messages", List.of());

        if (history.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Messages vides"));
        }

        // Injecter le contexte pédagogique dans le premier message si fourni
        List<Map<String, String>> messages = new ArrayList<>(history);
        if (!context.isBlank() && messages.size() == 1) {
            String enriched = "[Contexte : niveau " + level + " — " + context + "]\n\n" +
                              messages.get(0).get("content");
            messages.set(0, Map.of("role", "user", "content", enriched));
        }

        String reply = callClaude(messages, 500);
        if (reply == null) return serverError();
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    // ─── Endpoint génération de QCM ─────────────────────────────────────────
    @PostMapping("/generate-qcm")
    public ResponseEntity<?> generateQcm(@RequestBody Map<String, String> body) {
        if (!isConfigured()) return unavailable();

        String level  = body.getOrDefault("level", "A1");
        String theme  = body.getOrDefault("theme", "vocabulaire général");
        String count  = body.getOrDefault("count", "5");

        String prompt = "Génère exactement " + count + " questions QCM d'allemand pour le niveau " + level +
            " sur le thème : " + theme + ".\n\n" +
            "Format STRICT (JSON valide) :\n" +
            "[\n" +
            "  {\n" +
            "    \"question\": \"...\",\n" +
            "    \"choices\": [\"A) ...\", \"B) ...\", \"C) ...\", \"D) ...\"],\n" +
            "    \"correct\": 0,\n" +
            "    \"explanation\": \"...\"\n" +
            "  }\n" +
            "]\n\n" +
            "- Les questions doivent être en français ou allemand selon le thème\n" +
            "- L'index 'correct' est 0-based (0=A, 1=B, 2=C, 3=D)\n" +
            "- L'explication est en français\n" +
            "- Adapte la difficulté au niveau " + level + "\n" +
            "Réponds UNIQUEMENT avec le JSON, sans texte autour.";

        String reply = callClaude(List.of(Map.of("role", "user", "content", prompt)), 1000);
        if (reply == null) return serverError();
        return ResponseEntity.ok(Map.of("qcm", reply));
    }

    // ─── Appel Claude API ────────────────────────────────────────────────────
    private String callClaude(List<Map<String, String>> messages, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> requestBody = Map.of(
            "model",      "claude-haiku-4-5-20251001",
            "max_tokens", maxTokens,
            "system",     SYSTEM_PROMPT,
            "messages",   messages
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.anthropic.com/v1/messages",
                new HttpEntity<>(requestBody, headers),
                Map.class
            );
            Map<?, ?> rb = response.getBody();
            if (rb != null && rb.containsKey("content")) {
                List<?> content = (List<?>) rb.get("content");
                if (!content.isEmpty()) {
                    return (String) ((Map<?, ?>) content.get(0)).get("text");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ─── Message utilisateur selon le mode ───────────────────────────────────
    private String buildUserMessage(String level, String question, String context, String mode) {
        String lvl = "[Niveau " + level + "] ";
        return switch (mode) {
            case "explain"   -> lvl + "Explique ce concept ou cette règle de grammaire : " + question +
                                (context.isBlank() ? "" : "\nContexte : " + context);
            case "correct"   -> lvl + "Corrige cette phrase/réponse et explique l'erreur : \"" + question + "\"" +
                                (context.isBlank() ? "" : "\nQuestion posée : " + context);
            case "translate" -> lvl + "Traduis et explique cette phrase allemande : \"" + question + "\"";
            case "practice"  -> lvl + "Crée 2 phrases d'exemple en allemand (avec traduction) pour illustrer : " + question;
            case "pronounce" -> lvl + "Conseil de prononciation : " + question +
                                (context.isBlank() ? "" : "\n" + context);
            default          -> lvl + question;
        };
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private ResponseEntity<?> unavailable() {
        return ResponseEntity.status(503).body(Map.of(
            "error", "Tuteur IA non configuré. Ajoutez ANTHROPIC_API_KEY dans application.yml"
        ));
    }

    private ResponseEntity<?> serverError() {
        return ResponseEntity.status(500).body(Map.of("error", "Erreur lors de la communication avec l'IA"));
    }
}
