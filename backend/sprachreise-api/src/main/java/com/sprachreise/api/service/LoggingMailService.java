package com.sprachreise.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class LoggingMailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${admin.email:admin@sprachreise.com}")
    private String adminEmail;

    public LoggingMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private void sendHtml(String to, String subject, String htmlBody) {
        log.info("===== EMAIL HTML =====\nTo: {}\nSubject: {}\n======================", to, subject);
        if (mailSender == null || to == null || to.isBlank()) return;
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            if (fromAddress != null && !fromAddress.isBlank() && !fromAddress.contains("your-"))
                helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
            log.info("HTML email sent to {}", to);
        } catch (Exception e) {
            log.warn("HTML SMTP send failed for {} : {}", to, e.getMessage());
        }
    }

    private void send(String to, String subject, String body) {
        log.info("===== EMAIL =====");
        log.info("To      : {}", to);
        log.info("Subject : {}", subject);
        log.info("Body    :\n{}", body);
        log.info("=================");
        if (mailSender == null) { log.warn("JavaMailSender is null, email NOT sent."); return; }
        if (to == null || to.isBlank()) { log.warn("Recipient empty, email NOT sent."); return; }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank() && !fromAddress.contains("your-"))
                msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to {} via SMTP.", to);
        } catch (Exception e) {
            log.warn("SMTP send failed for {} : {}", to, e.getMessage());
        }
    }

    public void notifyExamSubmission(String trainerEmail, String learnerName, String examTitle) {
        send(trainerEmail,
            "Nouvelle copie reçue pour l'épreuve « " + examTitle + " »",
            String.format("L'apprenant %s vient de soumettre une copie pour votre épreuve « %s ».\n\n— SprachReise",
                learnerName, examTitle));
    }

    public void notifyExamGraded(String learnerEmail, String examTitle,
                                 java.math.BigDecimal grade, String feedback) {
        send(learnerEmail,
            "Votre copie « " + examTitle + " » a été corrigée",
            String.format("Votre épreuve « %s » a été corrigée.\n\nNote : %s/20\nFeedback :\n%s\n\n— SprachReise",
                examTitle, grade, feedback == null ? "" : feedback));
    }

    public void notifyLearnersSessionScheduled(Long levelId, String title,
                                               java.time.LocalDateTime scheduledStart) {
        log.info("===== PUSH [LEARNERS level_id={}] : SESSION SCHEDULED : {} @ {} =====",
            levelId, title, scheduledStart);
    }

    public void sendTrainerApproval(String email, String firstName) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName : "formateur";
        String subject = "SprachReise — Votre candidature formateur a été approuvée !";
        String html = """
            <div style="font-family:'Georgia',serif;max-width:520px;margin:0 auto;background:#F5EFE3;border:1px solid #D9CAAA;border-radius:8px;overflow:hidden;">
              <div style="background:#372619;padding:32px 28px;text-align:center;">
                <h1 style="color:#F9F4E8;font-size:28px;margin:0;font-style:italic;font-weight:400;">SprachReise</h1>
                <p style="color:#B8893A;font-size:11px;letter-spacing:3px;margin:6px 0 0;text-transform:uppercase;">Voyage en langue allemande</p>
              </div>
              <div style="padding:36px 32px;">
                <div style="text-align:center;margin-bottom:24px;">
                  <div style="font-size:48px;">✓</div>
                  <h2 style="color:#372619;font-size:22px;margin:12px 0 4px;">Candidature approuvée !</h2>
                  <div style="width:40px;height:2px;background:#B8893A;margin:0 auto;"></div>
                </div>
                <p style="color:#1F1610;font-size:15px;line-height:1.7;">Bonjour <strong>%s</strong>,</p>
                <p style="color:#1F1610;font-size:15px;line-height:1.7;">
                  Bonne nouvelle ! Votre candidature formateur sur <strong>SprachReise</strong> a été approuvée.
                  Vous pouvez maintenant accéder à votre espace formateur et commencer à publier vos cours.
                </p>
                <div style="background:#372619;border-radius:6px;padding:20px;margin:24px 0;text-align:center;">
                  <p style="color:#AE9182;font-size:12px;margin:0 0 4px;letter-spacing:2px;text-transform:uppercase;">Vos identifiants</p>
                  <p style="color:#F9F4E8;font-size:15px;margin:4px 0;"><strong>Email :</strong> %s</p>
                  <p style="color:#AE9182;font-size:12px;margin:8px 0 0;">Utilisez le mot de passe que vous avez choisi lors de votre inscription.</p>
                </div>
                <div style="text-align:center;margin:28px 0;">
                  <a href="exp://sprachreise/login" style="display:inline-block;background:#A15E2D;color:#F9F4E8;text-decoration:none;padding:14px 32px;border-radius:6px;font-size:13px;letter-spacing:2px;font-family:'Arial',sans-serif;font-weight:bold;">
                    SE CONNECTER
                  </a>
                  <p style="color:#AE9182;font-size:11px;margin:12px 0 0;">Ouvrez SprachReise sur votre téléphone et connectez-vous avec vos identifiants.</p>
                </div>
                <p style="color:#1F1610;font-size:15px;line-height:1.7;">Bienvenue dans l'équipe SprachReise !</p>
              </div>
              <div style="background:#EAE0CC;padding:16px 28px;text-align:center;">
                <p style="color:#AE9182;font-size:11px;margin:0;">— L'équipe SprachReise &nbsp;·&nbsp; <em>Lernen ohne Grenzen</em></p>
              </div>
            </div>
            """.formatted(name, email);
        sendHtml(email, subject, html);
    }

    public void sendTrainerRejection(String email, String firstName, String motif) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName : "candidat";
        String subject = "SprachReise — Réponse concernant votre candidature formateur";
        String html = """
            <div style="font-family:'Georgia',serif;max-width:520px;margin:0 auto;background:#F5EFE3;border:1px solid #D9CAAA;border-radius:8px;overflow:hidden;">
              <div style="background:#372619;padding:32px 28px;text-align:center;">
                <h1 style="color:#F9F4E8;font-size:28px;margin:0;font-style:italic;font-weight:400;">SprachReise</h1>
                <p style="color:#B8893A;font-size:11px;letter-spacing:3px;margin:6px 0 0;text-transform:uppercase;">Voyage en langue allemande</p>
              </div>
              <div style="padding:36px 32px;">
                <p style="color:#1F1610;font-size:15px;line-height:1.7;">Bonjour <strong>%s</strong>,</p>
                <p style="color:#1F1610;font-size:15px;line-height:1.7;">
                  Après examen attentif de votre dossier, nous ne pouvons malheureusement pas donner suite à votre candidature formateur pour le moment.
                </p>
                <div style="background:#fff0f0;border-left:4px solid #EF4444;border-radius:4px;padding:14px 18px;margin:20px 0;">
                  <p style="color:#7f1d1d;font-size:13px;margin:0;"><strong>Motif :</strong> %s</p>
                </div>
                <p style="color:#1F1610;font-size:15px;line-height:1.7;">
                  Vous pouvez soumettre une nouvelle candidature avec un dossier complet à tout moment.
                </p>
              </div>
              <div style="background:#EAE0CC;padding:16px 28px;text-align:center;">
                <p style="color:#AE9182;font-size:11px;margin:0;">— L'équipe SprachReise &nbsp;·&nbsp; <em>Lernen ohne Grenzen</em></p>
              </div>
            </div>
            """.formatted(name, motif);
        sendHtml(email, subject, html);
    }

    public void sendPasswordReset(String email, String firstName, String newPassword) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName : "utilisateur";
        send(email,
            "SprachReise — Votre nouveau mot de passe",
            String.format("""
                Bonjour %s,

                Vous avez demandé la réinitialisation de votre mot de passe SprachReise.

                Votre nouveau mot de passe temporaire :
                  %s

                Connectez-vous avec ce mot de passe, puis changez-le depuis votre profil.

                Si vous n'avez pas fait cette demande, ignorez ce message.

                — L'équipe SprachReise
                """, name, newPassword));
    }

}
