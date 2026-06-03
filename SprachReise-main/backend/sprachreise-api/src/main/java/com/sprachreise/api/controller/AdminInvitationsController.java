package com.sprachreise.api.controller;

import com.sprachreise.api.entity.TrainerInvitation;
import com.sprachreise.api.repository.TrainerInvitationRepository;
import com.sprachreise.api.service.InvitationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/invitations")
public class AdminInvitationsController {

    private static final Pattern EMAIL_REGEX =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final TrainerInvitationRepository invitationRepository;
    private final InvitationService invitationService;

    public AdminInvitationsController(TrainerInvitationRepository invitationRepository,
                                      InvitationService invitationService) {
        this.invitationRepository = invitationRepository;
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<?> sendBatch(@RequestBody Map<String, Object> body) {
        Object rawEmails = body.get("emails");
        if (!(rawEmails instanceof List<?>)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Champ 'emails' requis (liste)"));
        }
        String template = body.get("template") == null ? "FR" : body.get("template").toString().toUpperCase();
        if (!template.equals("FR") && !template.equals("DE")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Template invalide (FR|DE)"));
        }

        List<String> valid = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        int skipped = 0;
        for (Object o : (List<?>) rawEmails) {
            if (o == null) { skipped++; continue; }
            String e = o.toString().trim().toLowerCase();
            if (e.isEmpty() || !EMAIL_REGEX.matcher(e).matches()) { skipped++; continue; }
            if (!seen.add(e)) { skipped++; continue; }
            valid.add(e);
        }

        if (valid.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Aucun email valide", "skipped", skipped
            ));
        }

        invitationService.sendInvitations(valid, currentAdminEmail(), template);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "count", valid.size(),
            "scheduled", true,
            "skipped", skipped
        ));
    }

    @PostMapping("/import-csv")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "template", defaultValue = "FR") String template) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier CSV requis"));
        }
        String tpl = template == null ? "FR" : template.toUpperCase();
        if (!tpl.equals("FR") && !tpl.equals("DE")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Template invalide (FR|DE)"));
        }

        LinkedHashSet<String> seen = new LinkedHashSet<>();
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("[,;]");
                for (String p : parts) {
                    String e = p == null ? "" : p.trim().toLowerCase();
                    // Strip surrounding quotes
                    if (e.startsWith("\"") && e.endsWith("\"") && e.length() >= 2) {
                        e = e.substring(1, e.length() - 1).trim();
                    }
                    if (e.isEmpty()) continue;
                    if (!EMAIL_REGEX.matcher(e).matches()) { skipped++; continue; }
                    if (!seen.add(e)) { skipped++; }
                }
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Lecture CSV impossible : " + e.getMessage()));
        }

        List<String> valid = new ArrayList<>(seen);
        if (valid.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Aucun email valide trouvé dans le CSV", "skipped", skipped
            ));
        }

        invitationService.sendInvitations(valid, currentAdminEmail(), tpl);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "count", valid.size(),
            "scheduled", true,
            "skipped", skipped
        ));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        // Refresh expired statuses on read
        LocalDateTime now = LocalDateTime.now();
        List<TrainerInvitation> all = invitationRepository.findAllByOrderBySentAtDesc();
        List<Map<String, Object>> rows = all.stream().map(inv -> {
            String status = inv.getStatus().name();
            if (inv.getStatus() == TrainerInvitation.Status.SENT
                    && inv.getExpiresAt() != null
                    && inv.getExpiresAt().isBefore(now)) {
                status = TrainerInvitation.Status.EXPIRED.name();
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", inv.getId());
            m.put("email", inv.getEmail());
            m.put("token", inv.getToken());
            m.put("sentBy", inv.getSentBy());
            m.put("status", status);
            m.put("expiresAt", inv.getExpiresAt());
            m.put("sentAt", inv.getSentAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(rows);
    }

    private static String currentAdminEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails)) return null;
        return ((UserDetails) principal).getUsername();
    }
}
