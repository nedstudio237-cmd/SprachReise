package com.sprachreise.api.controller;

import com.sprachreise.api.entity.Certificate;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.CertificateRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.service.CertificateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService    certService;
    private final CertificateRepository certRepo;
    private final UserRepository        userRepo;

    @Value("${storage.upload-dir}")
    private String storageDir;

    public CertificateController(CertificateService certService,
                                  CertificateRepository certRepo,
                                  UserRepository userRepo) {
        this.certService = certService;
        this.certRepo    = certRepo;
        this.userRepo    = userRepo;
    }

    // ── Mes certificats (apprenant) ───────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<?> mine(@AuthenticationPrincipal UserDetails ud) {
        User me = userRepo.findByEmail(ud.getUsername()).orElse(null);
        if (me == null) return ResponseEntity.status(401).build();

        List<Certificate> certs = certRepo.findAllByLearnerIdOrderByIssuedAtDesc(me.getId());
        return ResponseEntity.ok(certs.stream().map(this::toDto).toList());
    }

    // ── Certificats d'un apprenant (admin/trainer) ────────────────────────────
    @GetMapping("/learner/{learnerId}")
    public ResponseEntity<?> byLearner(@PathVariable Long learnerId,
                                        @AuthenticationPrincipal UserDetails ud) {
        List<Certificate> certs = certRepo.findAllByLearnerIdOrderByIssuedAtDesc(learnerId);
        return ResponseEntity.ok(certs.stream().map(this::toDto).toList());
    }

    // ── Télécharger un certificat PDF ─────────────────────────────────────────
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                              @AuthenticationPrincipal UserDetails ud) {
        User me = userRepo.findByEmail(ud.getUsername()).orElse(null);
        if (me == null) return ResponseEntity.status(401).build();

        Certificate cert = certRepo.findById(id).orElse(null);
        if (cert == null) return ResponseEntity.notFound().build();

        // Seul l'apprenant concerné ou un admin peut télécharger
        boolean isAdmin   = me.getRole() != null && me.getRole().name().equals("ADMIN");
        boolean isOwner   = cert.getLearnerId().equals(me.getId());
        boolean isTrainer = me.getRole() != null && me.getRole().name().equals("TRAINER");
        if (!isAdmin && !isOwner && !isTrainer) return ResponseEntity.status(403).build();

        File file = new File(storageDir + "/" + cert.getPdfPath());
        if (!file.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"Certificat_SprachReise_" + cert.getLevelCode() + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(resource);
    }

    // ── Admin : apprenants éligibles ──────────────────────────────────────────
    @GetMapping("/admin/eligible")
    public ResponseEntity<?> eligible(@AuthenticationPrincipal UserDetails ud) {
        User me = userRepo.findByEmail(ud.getUsername()).orElse(null);
        if (me == null || !me.getRole().name().equals("ADMIN"))
            return ResponseEntity.status(403).build();
        return ResponseEntity.ok(certService.getEligibleLearners());
    }

    // ── Admin : émettre un certificat ────────────────────────────────────────
    @PostMapping("/admin/emit/{learnerId}")
    public ResponseEntity<?> emit(@PathVariable Long learnerId,
                                   @AuthenticationPrincipal UserDetails ud) {
        User admin = userRepo.findByEmail(ud.getUsername()).orElse(null);
        if (admin == null || !admin.getRole().name().equals("ADMIN"))
            return ResponseEntity.status(403).build();
        try {
            Certificate cert = certService.emit(learnerId, admin.getId());
            return ResponseEntity.ok(toDto(cert));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Admin : révoquer un certificat ───────────────────────────────────────
    @PostMapping("/admin/revoke/{certId}")
    public ResponseEntity<?> revoke(@PathVariable Long certId,
                                     @RequestBody Map<String, String> body,
                                     @AuthenticationPrincipal UserDetails ud) {
        User admin = userRepo.findByEmail(ud.getUsername()).orElse(null);
        if (admin == null || !admin.getRole().name().equals("ADMIN"))
            return ResponseEntity.status(403).build();

        String reason = body.getOrDefault("reason", "Révocation administrative");
        certService.revoke(certId, admin.getId(), reason);
        return ResponseEntity.ok(Map.of("message", "Certificat révoqué", "certId", certId));
    }

    // ── Admin : tous les certificats ──────────────────────────────────────────
    @GetMapping("/admin/all")
    public ResponseEntity<?> allCerts(@AuthenticationPrincipal UserDetails ud) {
        User admin = userRepo.findByEmail(ud.getUsername()).orElse(null);
        if (admin == null || !admin.getRole().name().equals("ADMIN"))
            return ResponseEntity.status(403).build();

        List<Certificate> certs = certRepo.findAllByRevokedFalseOrderByIssuedAtDesc();
        return ResponseEntity.ok(certs.stream().map(this::toDto).toList());
    }

    // ── DTO ───────────────────────────────────────────────────────────────────
    private Map<String, Object> toDto(Certificate c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",                c.getId());
        m.put("learnerId",         c.getLearnerId());
        m.put("levelCode",         c.getLevelCode());
        m.put("certificateNumber", c.getCertificateNumber());
        m.put("pdfSha256",         c.getPdfSha256());
        m.put("trainerId",         c.getTrainerId());
        m.put("issuedAt",          c.getIssuedAt() != null
            ? c.getIssuedAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)) : null);
        m.put("revoked",           c.getRevoked());
        m.put("revokeReason",      c.getRevokeReason());
        // Ajouter le nom de l'apprenant
        userRepo.findById(c.getLearnerId()).ifPresent(u -> {
            m.put("learnerName", u.getFirstName() + " " + u.getLastName());
            m.put("learnerEmail", u.getEmail());
        });
        return m;
    }
}
