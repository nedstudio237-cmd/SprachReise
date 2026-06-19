package com.sprachreise.api.controller;

import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.service.SubscriptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final SubscriptionService subService;
    private final UserRepository      userRepo;

    public SubscriptionController(SubscriptionService subService, UserRepository userRepo) {
        this.subService = subService;
        this.userRepo   = userRepo;
    }

    private User me(UserDetails ud) {
        return userRepo.findByEmail(ud.getUsername()).orElse(null);
    }

    // ── Statut abonnement ─────────────────────────────────────────────────────
    @GetMapping("/status")
    public ResponseEntity<?> status(@AuthenticationPrincipal UserDetails ud) {
        User user = me(ud);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(subService.getStatus(user));
    }

    // ── Activer l'essai gratuit (appelé à l'inscription) ─────────────────────
    @PostMapping("/trial")
    public ResponseEntity<?> trial(@AuthenticationPrincipal UserDetails ud) {
        User user = me(ud);
        if (user == null) return ResponseEntity.status(401).build();
        subService.activateTrial(user);
        return ResponseEntity.ok(Map.of("message", "Essai gratuit activé", "days", 7));
    }

    // ── Initier paiement Stripe ───────────────────────────────────────────────
    @PostMapping("/pay/stripe")
    public ResponseEntity<?> payStripe(@RequestBody Map<String, String> body,
                                        @AuthenticationPrincipal UserDetails ud) {
        User user = me(ud);
        if (user == null) return ResponseEntity.status(401).build();
        String plan = body.get("plan");
        try {
            return ResponseEntity.ok(subService.initiateStripe(user.getId(), plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Initier paiement CamPay (Orange / MTN) ───────────────────────────────
    @PostMapping("/pay/campay")
    public ResponseEntity<?> payCamPay(@RequestBody Map<String, String> body,
                                        @AuthenticationPrincipal UserDetails ud) {
        User user = me(ud);
        if (user == null) return ResponseEntity.status(401).build();
        String plan     = body.get("plan");
        String phone    = body.get("phone");
        String operator = body.get("operator"); // ORANGE ou MTN
        if (phone == null || phone.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Numéro de téléphone requis"));
        try {
            return ResponseEntity.ok(subService.initiateCamPay(user.getId(), plan, phone, operator));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Initier paiement PayPal ───────────────────────────────────────────────
    @PostMapping("/pay/paypal")
    public ResponseEntity<?> payPayPal(@RequestBody Map<String, String> body,
                                        @AuthenticationPrincipal UserDetails ud) {
        User user = me(ud);
        if (user == null) return ResponseEntity.status(401).build();
        String plan = body.get("plan");
        try {
            return ResponseEntity.ok(subService.initiatePayPal(user.getId(), plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Confirmer un paiement (succès côté client) ───────────────────────────
    @PostMapping("/pay/confirm")
    public ResponseEntity<?> confirm(@RequestBody Map<String, String> body,
                                      @AuthenticationPrincipal UserDetails ud) {
        User user = me(ud);
        if (user == null) return ResponseEntity.status(401).build();
        String ref = body.get("externalRef");
        if (ref == null) return ResponseEntity.badRequest().body(Map.of("error", "Référence requise"));
        try {
            return ResponseEntity.ok(subService.confirmPayment(user.getId(), ref));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
