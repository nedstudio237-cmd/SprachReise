package com.sprachreise.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiTutorController {

    @Value("${anthropic.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/explain")
    public ResponseEntity<?> explain(@RequestBody Map<String, String> body) {
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Tuteur IA non configuré — ajoutez la clé API dans application.yml"
            ));
        }

        String level    = body.getOrDefault("level", "A1");
        String question = body.getOrDefault("question", "");
        String context  = body.getOrDefault("context", "");
        String mode     = body.getOrDefault("mode", "explain");

        String prompt = buildPrompt(level, question, context, mode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> requestBody = Map.of(
            "model", "claude-haiku-4-5-20251001",
            "max_tokens", 600,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.anthropic.com/v1/messages", entity, Map.class
            );
            Map<?, ?> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("content")) {
                List<?> content = (List<?>) responseBody.get("content");
                if (!content.isEmpty()) {
                    Map<?, ?> first = (Map<?, ?>) content.get(0);
                    String text = (String) first.get("text");
                    return ResponseEntity.ok(Map.of("explanation", text));
                }
            }
            return ResponseEntity.status(500).body(Map.of("error", "Réponse IA invalide"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur IA : " + e.getMessage()));
        }
    }

    private String buildPrompt(String level, String question, String context, String mode) {
        String base = "Tu es un professeur d'allemand expert qui enseigne à des apprenants francophones d'Afrique (niveau " + level + "). " +
                      "Tu expliques en français, clairement et avec bienveillance. " +
                      "Sois concis (max 200 mots). Utilise des exemples simples avec traduction.";

        return switch (mode) {
            case "explain" -> base + "\n\nExplique la règle de grammaire ou le concept suivant : " + question +
                              (context.isBlank() ? "" : "\nContexte : " + context);
            case "correct" -> base + "\n\nCorrige cette réponse d'apprenant et explique pourquoi : \"" + question + "\"" +
                              (context.isBlank() ? "" : "\nQuestion posée : " + context);
            case "translate" -> base + "\n\nTraduis et explique cette phrase allemande : \"" + question + "\"";
            case "practice" -> base + "\n\nCrée une phrase d'exemple en allemand avec sa traduction pour illustrer : " + question;
            default -> base + "\n\n" + question;
        };
    }
}
