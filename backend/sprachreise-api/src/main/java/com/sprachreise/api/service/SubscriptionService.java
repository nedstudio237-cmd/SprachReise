package com.sprachreise.api.service;

import com.sprachreise.api.entity.Payment;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.PaymentRepository;
import com.sprachreise.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ArrayList;

@Service
public class SubscriptionService {

    // Prix en FCFA
    public static final Map<String, BigDecimal> PRICES = Map.of(
        "BASIC",    new BigDecimal("3000"),
        "STANDARD", new BigDecimal("7500"),
        "PREMIUM",  new BigDecimal("15000")
    );

    public static final Map<String, String> PLAN_NAMES = Map.of(
        "BASIC",    "Apprenti",
        "STANDARD", "Voyageur",
        "PREMIUM",  "Érudit"
    );

    private final UserRepository    userRepo;
    private final PaymentRepository paymentRepo;
    private final JavaMailSender    mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${stripe.secret-key:sk_test_placeholder}")
    private String stripeSecretKey;

    @Value("${campay.api-key:campay_test_placeholder}")
    private String campayApiKey;

    @Value("${campay.app-username:campay_user_placeholder}")
    private String campayUsername;

    public SubscriptionService(UserRepository userRepo,
                                PaymentRepository paymentRepo,
                                JavaMailSender mailSender) {
        this.userRepo    = userRepo;
        this.paymentRepo = paymentRepo;
        this.mailSender  = mailSender;
    }

    // ── Activer l'essai gratuit de 7 jours ───────────────────────────────────
    public void activateTrial(User user) {
        user.setSubscriptionPlan("TRIAL");
        user.setSubscriptionStatus("TRIAL");
        user.setTrialEndsAt(LocalDateTime.now().plusDays(7));
        user.setReminder5dSent(false);
        user.setReminder0dSent(false);
        userRepo.save(user);
        sendTrialWelcomeEmail(user);
    }

    // ── Initier un paiement Stripe (Visa/Mastercard) ─────────────────────────
    public Map<String, Object> initiateStripe(Long learnerId, String plan) {
        User user = userRepo.findById(learnerId).orElseThrow();
        BigDecimal amount = PRICES.get(plan);
        if (amount == null) throw new RuntimeException("Plan inconnu : " + plan);

        // Simuler Stripe Payment Intent (en mode test, la vraie intégration utilise stripe-java)
        // En production : utiliser com.stripe.model.PaymentIntent.create(...)
        String fakeClientSecret = "pi_test_" + UUID.randomUUID().toString().replace("-", "") + "_secret_test";
        String intentId = "pi_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        Payment payment = new Payment();
        payment.setLearnerId(learnerId);
        payment.setPlan(plan);
        payment.setMethod("STRIPE");
        payment.setAmount(amount);
        payment.setCurrency("XAF");
        payment.setStatus("PENDING");
        payment.setExternalRef(intentId);
        paymentRepo.save(payment);

        return Map.of(
            "clientSecret",   fakeClientSecret,
            "paymentIntentId", intentId,
            "amount",          amount,
            "currency",        "XAF",
            "plan",            plan,
            "planName",        PLAN_NAMES.get(plan),
            "mode",            "test" // indique mode sandbox
        );
    }

    // ── Initier un paiement CamPay (Orange Money / MTN Money) ────────────────
    public Map<String, Object> initiateCamPay(Long learnerId, String plan,
                                               String phoneNumber, String operator) {
        User user = userRepo.findById(learnerId).orElseThrow();
        BigDecimal amount = PRICES.get(plan);
        if (amount == null) throw new RuntimeException("Plan inconnu : " + plan);

        // CamPay API — collect endpoint
        // En production : POST https://demo.campay.net/api/collect/
        // Pour la démo, on simule la réponse
        String reference = "CAMPAY_" + operator + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        Payment payment = new Payment();
        payment.setLearnerId(learnerId);
        payment.setPlan(plan);
        payment.setMethod("CAMPAY_" + operator); // CAMPAY_ORANGE ou CAMPAY_MTN
        payment.setAmount(amount);
        payment.setCurrency("XAF");
        payment.setStatus("PENDING");
        payment.setExternalRef(reference);
        payment.setPhoneNumber(phoneNumber);
        paymentRepo.save(payment);

        // Simuler le push USSD envoyé au téléphone
        return Map.of(
            "reference",   reference,
            "amount",      amount,
            "phone",       phoneNumber,
            "operator",    operator,
            "plan",        plan,
            "planName",    PLAN_NAMES.get(plan),
            "message",     "Confirmez le paiement de " + amount.intValue() + " FCFA sur votre téléphone " + phoneNumber,
            "mode",        "demo"
        );
    }

    // ── Initier un paiement PayPal (sandbox) ─────────────────────────────────
    public Map<String, Object> initiatePayPal(Long learnerId, String plan) {
        User user = userRepo.findById(learnerId).orElseThrow();
        BigDecimal amountFcfa = PRICES.get(plan);
        if (amountFcfa == null) throw new RuntimeException("Plan inconnu : " + plan);

        // Convertir FCFA → EUR (taux approximatif : 1 EUR ≈ 655 FCFA)
        BigDecimal amountEur = amountFcfa.divide(new BigDecimal("655"), 2, java.math.RoundingMode.HALF_UP);

        // PayPal sandbox — en prod : utiliser paypal-checkout-sdk
        String orderId = "PP_SANDBOX_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();

        Payment payment = new Payment();
        payment.setLearnerId(learnerId);
        payment.setPlan(plan);
        payment.setMethod("PAYPAL");
        payment.setAmount(amountEur);
        payment.setCurrency("EUR");
        payment.setStatus("PENDING");
        payment.setExternalRef(orderId);
        paymentRepo.save(payment);

        // URL d'approbation sandbox PayPal (simulée)
        return Map.of(
            "orderId",      orderId,
            "amountEur",    amountEur,
            "amountFcfa",   amountFcfa,
            "plan",         plan,
            "planName",     PLAN_NAMES.get(plan),
            "approvalUrl",  "https://www.sandbox.paypal.com/checkoutnow?token=" + orderId,
            "mode",         "sandbox"
        );
    }

    // ── Confirmer un paiement (appelé après succès côté client) ──────────────
    public Map<String, Object> confirmPayment(Long learnerId, String externalRef) {
        Payment payment = paymentRepo.findByExternalRefAndStatus(externalRef, "PENDING")
            .orElseThrow(() -> new RuntimeException("Paiement introuvable ou déjà traité"));

        if (!payment.getLearnerId().equals(learnerId))
            throw new RuntimeException("Accès refusé");

        // Marquer comme succès
        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepo.save(payment);

        // Activer l'abonnement (30 jours)
        User user = userRepo.findById(learnerId).orElseThrow();
        user.setSubscriptionPlan(payment.getPlan());
        user.setSubscriptionStatus("ACTIVE");
        user.setSubscriptionStartsAt(LocalDateTime.now());
        user.setSubscriptionEndsAt(LocalDateTime.now().plusDays(30));
        user.setReminder5dSent(false);
        user.setReminder0dSent(false);
        userRepo.save(user);

        // Email de confirmation
        sendPaymentConfirmationEmail(user, payment);

        return Map.of(
            "status",     "SUCCESS",
            "plan",       payment.getPlan(),
            "planName",   PLAN_NAMES.getOrDefault(payment.getPlan(), payment.getPlan()),
            "expiresAt",  user.getSubscriptionEndsAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)),
            "method",     payment.getMethod()
        );
    }

    // ── Scheduler : vérifier les abonnements chaque jour à 8h ────────────────
    @Scheduled(cron = "0 0 8 * * *")
    public void checkSubscriptions() {
        LocalDateTime now = LocalDateTime.now();

        List<User> users = userRepo.findAll();
        for (User u : users) {
            if (!"LEARNER".equals(u.getRole() != null ? u.getRole().name() : "")) continue;

            // Rappel J-5 avant fin d'abonnement
            if ("ACTIVE".equals(u.getSubscriptionStatus())
                && u.getSubscriptionEndsAt() != null
                && !Boolean.TRUE.equals(u.getReminder5dSent())) {
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, u.getSubscriptionEndsAt());
                if (daysLeft <= 5 && daysLeft > 0) {
                    sendReminderEmail(u, (int) daysLeft);
                    u.setReminder5dSent(true);
                    userRepo.save(u);
                }
            }

            // Rappel J-0 (jour d'expiration)
            if ("ACTIVE".equals(u.getSubscriptionStatus())
                && u.getSubscriptionEndsAt() != null
                && !Boolean.TRUE.equals(u.getReminder0dSent())) {
                if (now.isAfter(u.getSubscriptionEndsAt())) {
                    sendExpirationEmail(u);
                    u.setReminder0dSent(true);
                    u.setSubscriptionStatus("EXPIRED");
                    userRepo.save(u);
                }
            }

            // Essai gratuit expiré
            if ("TRIAL".equals(u.getSubscriptionStatus())
                && u.getTrialEndsAt() != null
                && now.isAfter(u.getTrialEndsAt())) {
                u.setSubscriptionStatus("EXPIRED");
                userRepo.save(u);
                sendTrialExpiredEmail(u);
            }
        }
    }

    // ── Statut abonnement d'un utilisateur ───────────────────────────────────
    public Map<String, Object> getStatus(User user) {
        // Normaliser le statut pour les anciens comptes sans abonnement
        String status = user.getSubscriptionStatus() != null ? user.getSubscriptionStatus() : "NONE";
        String plan   = user.getSubscriptionPlan()   != null ? user.getSubscriptionPlan()   : "NONE";

        Map<String, Object> m = new HashMap<>();
        m.put("plan",    plan);
        m.put("status",  status);
        m.put("trialEndsAt",        user.getTrialEndsAt() != null
            ? user.getTrialEndsAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)) : "");
        m.put("subscriptionEndsAt", user.getSubscriptionEndsAt() != null
            ? user.getSubscriptionEndsAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)) : "");
        m.put("planName", PLAN_NAMES.getOrDefault(plan, "Aucun abonnement"));

        // Calculer les jours restants
        if ("ACTIVE".equals(status) && user.getSubscriptionEndsAt() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), user.getSubscriptionEndsAt());
            m.put("daysLeft", Math.max(0, days));
        } else if ("TRIAL".equals(status) && user.getTrialEndsAt() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), user.getTrialEndsAt());
            m.put("daysLeft", Math.max(0, days));
        } else {
            m.put("daysLeft", 0);
        }

        // Historique des paiements — HashMap pour autoriser les valeurs null
        List<Map<String, Object>> payHistory = new ArrayList<>();
        for (Payment p : paymentRepo.findAllByLearnerIdOrderByCreatedAtDesc(user.getId())) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id",       p.getId());
            entry.put("plan",     p.getPlan() != null ? p.getPlan() : "");
            entry.put("method",   p.getMethod() != null ? p.getMethod() : "");
            entry.put("amount",   p.getAmount() != null ? p.getAmount().toPlainString() : "0");
            entry.put("currency", p.getCurrency() != null ? p.getCurrency() : "XAF");
            entry.put("status",   p.getStatus() != null ? p.getStatus() : "");
            entry.put("date",     p.getCreatedAt() != null
                ? p.getCreatedAt().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)) : "");
            payHistory.add(entry);
        }
        m.put("payments", payHistory);
        return m;
    }

    // ── Emails ────────────────────────────────────────────────────────────────
    @Async
    public void sendTrialWelcomeEmail(User user) {
        sendEmail(user.getEmail(),
            "🎉 Bienvenue sur SprachReise — Votre essai gratuit commence !",
            buildTrialWelcomeHtml(user));
    }

    @Async
    public void sendPaymentConfirmationEmail(User user, Payment payment) {
        sendEmail(user.getEmail(),
            "✅ Paiement confirmé — Abonnement " + PLAN_NAMES.getOrDefault(payment.getPlan(), payment.getPlan()) + " activé",
            buildPaymentConfirmHtml(user, payment));
    }

    @Async
    public void sendReminderEmail(User user, int daysLeft) {
        sendEmail(user.getEmail(),
            "⏰ Votre abonnement SprachReise expire dans " + daysLeft + " jour(s)",
            buildReminderHtml(user, daysLeft));
    }

    @Async
    public void sendExpirationEmail(User user) {
        sendEmail(user.getEmail(),
            "❗ Votre abonnement SprachReise a expiré — Renouvelez maintenant",
            buildExpirationHtml(user));
    }

    @Async
    public void sendTrialExpiredEmail(User user) {
        sendEmail(user.getEmail(),
            "⌛ Votre essai gratuit SprachReise est terminé",
            buildTrialExpiredHtml(user));
    }

    private void sendEmail(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Email error: " + e.getMessage());
        }
    }

    private String wrap(String body) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:'Georgia',serif;background:#F9F4E8;margin:0;padding:0;">
            <div style="max-width:600px;margin:32px auto;background:#fff;border:2px solid #B8893A;border-radius:8px;overflow:hidden;">
              <div style="background:#372619;padding:24px 32px;text-align:center;">
                <p style="color:#B8893A;font-size:13px;letter-spacing:4px;margin:0 0 4px;">SPRACHREISE</p>
                <p style="color:#F9F4E8;font-size:11px;font-style:italic;margin:0;">Lernen · Reisen · Entdecken</p>
              </div>
              <div style="padding:28px 32px;">%s</div>
              <div style="background:#F5EFE3;padding:14px 32px;text-align:center;border-top:1px solid #D9CAAA;">
                <p style="color:#AE9182;font-size:11px;margin:0;font-style:italic;">sprachreise.app — support@sprachreise.app</p>
              </div>
            </div>
            </body></html>
            """.formatted(body);
    }

    private String buildTrialWelcomeHtml(User u) {
        String end = u.getTrialEndsAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
        return wrap("""
            <p style="color:#372619;font-size:15px;">Bonjour <strong>%s</strong> 👋</p>
            <p style="color:#372619;font-size:14px;line-height:1.7;">
              Votre essai gratuit de <strong>7 jours</strong> est maintenant actif.<br>
              Profitez de l'accès complet à la plateforme SprachReise jusqu'au <strong style="color:#B8893A;">%s</strong>.
            </p>
            <div style="background:#F9F4E8;border-left:4px solid #B8893A;padding:14px 18px;margin:20px 0;border-radius:0 6px 6px 0;">
              <p style="margin:0;color:#372619;font-weight:bold;">Ce qui vous attend :</p>
              <ul style="margin:8px 0 0;color:#372619;font-size:13px;line-height:1.8;">
                <li>📚 Cours vidéos par niveau CECRL (A1 → C2)</li>
                <li>🎯 QCM et mini-jeux pédagogiques</li>
                <li>📡 Sessions live avec votre formateur</li>
                <li>🤖 Tuteur IA Max disponible 24h/24</li>
              </ul>
            </div>
            <p style="color:#AE9182;font-size:13px;font-style:italic;">
              À la fin de votre essai, choisissez un abonnement pour continuer votre voyage linguistique.
            </p>
            """.formatted(u.getFirstName(), end));
    }

    private String buildPaymentConfirmHtml(User u, Payment p) {
        String date = p.getPaidAt() != null ? p.getPaidAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy à HH:mm", Locale.FRENCH)) : "maintenant";
        String methodLabel = switch (p.getMethod()) {
            case "STRIPE" -> "Carte bancaire (Visa/Mastercard)";
            case "CAMPAY_ORANGE" -> "Orange Money";
            case "CAMPAY_MTN" -> "MTN Mobile Money";
            case "PAYPAL" -> "PayPal";
            default -> p.getMethod();
        };
        return wrap("""
            <p style="color:#372619;font-size:15px;">Bonjour <strong>%s</strong>,</p>
            <p style="color:#372619;font-size:14px;line-height:1.7;">Votre paiement a été <strong style="color:#10B981;">confirmé avec succès</strong>.</p>
            <div style="background:#F9F4E8;border:1px solid #D9CAAA;border-radius:8px;padding:16px 20px;margin:16px 0;">
              <table style="width:100%%;font-size:13px;color:#372619;">
                <tr><td>Plan</td><td style="text-align:right;font-weight:bold;">%s (%s)</td></tr>
                <tr><td>Montant</td><td style="text-align:right;">%s %s</td></tr>
                <tr><td>Moyen de paiement</td><td style="text-align:right;">%s</td></tr>
                <tr><td>Date</td><td style="text-align:right;">%s</td></tr>
              </table>
            </div>
            <p style="color:#AE9182;font-size:13px;font-style:italic;">
              Votre abonnement est valable 30 jours. Vous recevrez un rappel 5 jours avant l'expiration.
            </p>
            """.formatted(u.getFirstName(),
                PLAN_NAMES.getOrDefault(p.getPlan(), p.getPlan()), p.getPlan(),
                p.getAmount().toPlainString(), p.getCurrency(),
                methodLabel, date));
    }

    private String buildReminderHtml(User u, int days) {
        return wrap("""
            <p style="color:#372619;font-size:15px;">Bonjour <strong>%s</strong>,</p>
            <div style="background:#FEF3C7;border:1px solid #F59E0B;border-radius:8px;padding:16px 20px;margin:16px 0;text-align:center;">
              <p style="margin:0;color:#92400E;font-size:22px;font-weight:bold;">⏰ %d jour(s) restant(s)</p>
              <p style="margin:8px 0 0;color:#92400E;font-size:13px;">Votre abonnement <strong>%s</strong> expire bientôt.</p>
            </div>
            <p style="color:#372619;font-size:14px;line-height:1.7;">
              Pour continuer à accéder à vos cours, sessions live et à votre formateur,
              pensez à renouveler votre abonnement avant l'expiration.
            </p>
            <div style="text-align:center;margin:24px 0;">
              <a href="https://sprachreise.app/subscribe" style="background:#372619;color:#F9F4E8;padding:12px 28px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:14px;">
                Renouveler mon abonnement
              </a>
            </div>
            """.formatted(u.getFirstName(), days, PLAN_NAMES.getOrDefault(u.getSubscriptionPlan(), u.getSubscriptionPlan())));
    }

    private String buildExpirationHtml(User u) {
        return wrap("""
            <p style="color:#372619;font-size:15px;">Bonjour <strong>%s</strong>,</p>
            <div style="background:#FEE2E2;border:1px solid #EF4444;border-radius:8px;padding:16px 20px;margin:16px 0;text-align:center;">
              <p style="margin:0;color:#991B1B;font-size:18px;font-weight:bold;">❗ Votre abonnement a expiré</p>
            </div>
            <p style="color:#372619;font-size:14px;line-height:1.7;">
              Votre abonnement <strong>%s</strong> est arrivé à expiration.<br>
              Votre accès aux cours, sessions live et au tuteur IA est suspendu.
            </p>
            <div style="text-align:center;margin:24px 0;">
              <a href="https://sprachreise.app/subscribe" style="background:#EF4444;color:white;padding:12px 28px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:14px;">
                Renouveler maintenant
              </a>
            </div>
            <p style="color:#AE9182;font-size:12px;font-style:italic;text-align:center;">
              Vos données et votre progression sont conservées — reprenez là où vous vous étiez arrêté.
            </p>
            """.formatted(u.getFirstName(), PLAN_NAMES.getOrDefault(u.getSubscriptionPlan(), u.getSubscriptionPlan())));
    }

    private String buildTrialExpiredHtml(User u) {
        return wrap("""
            <p style="color:#372619;font-size:15px;">Bonjour <strong>%s</strong>,</p>
            <p style="color:#372619;font-size:14px;line-height:1.7;">
              Votre période d'essai gratuit de 7 jours est terminée.<br>
              Nous espérons que vous avez apprécié SprachReise !
            </p>
            <p style="color:#372619;font-size:14px;line-height:1.7;">
              Pour continuer votre voyage linguistique, choisissez l'abonnement qui vous convient :
            </p>
            <div style="background:#F9F4E8;border-radius:8px;padding:16px 20px;margin:16px 0;">
              <p style="margin:0 0 8px;font-weight:bold;color:#372619;">Nos formules :</p>
              <p style="margin:4px 0;color:#372619;font-size:13px;">🥉 <strong>Apprenti</strong> — 3 000 FCFA/mois · Cursus + QCM + profils formateurs</p>
              <p style="margin:4px 0;color:#B8893A;font-size:13px;">⭐ <strong>Voyageur</strong> — 7 500 FCFA/mois · + Vidéos + PDF + Évaluations</p>
              <p style="margin:4px 0;color:#372619;font-size:13px;">🏆 <strong>Érudit</strong> — 15 000 FCFA/mois · + Live + Certificats</p>
            </div>
            <div style="text-align:center;margin:24px 0;">
              <a href="https://sprachreise.app/subscribe" style="background:#B8893A;color:white;padding:12px 28px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:14px;">
                Choisir mon abonnement
              </a>
            </div>
            """.formatted(u.getFirstName()));
    }
}
