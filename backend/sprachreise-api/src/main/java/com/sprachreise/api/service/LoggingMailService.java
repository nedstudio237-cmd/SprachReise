package com.sprachreise.api.service;

import com.sprachreise.api.entity.TrainerApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends real emails via JavaMailSender. Always logs the email content
 * for traceability. If SMTP is misconfigured or sending fails, logs the
 * exception but never throws — the calling controller flow continues.
 */
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

    private void send(String to, String subject, String body) {
        log.info("===== EMAIL =====");
        log.info("To       : {}", to);
        log.info("Subject  : {}", subject);
        log.info("Body     :\n{}", body);
        log.info("=================");

        if (mailSender == null) {
            log.warn("JavaMailSender is null, email NOT sent.");
            return;
        }
        if (to == null || to.isBlank()) {
            log.warn("Recipient is empty, email NOT sent.");
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()
                    && !fromAddress.contains("your-")) {
                msg.setFrom(fromAddress);
            }
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to {} via SMTP.", to);
        } catch (Exception e) {
            log.warn("SMTP send failed for {} : {}", to, e.getMessage());
        }
    }

    public void notifyAdminNewApplication(TrainerApplication application) {
        String subject = "Nouvelle candidature formateur #" + application.getId();
        String body = String.format("""
                Bonjour,

                Une nouvelle candidature de formateur vient d'être soumise sur SprachReise.

                ID candidature : %d
                Nom            : %s %s
                Email          : %s
                Téléphone      : %s
                Niveau souhaité: level_id=%s
                Langue mat.    : %s

                Motivation :
                %s

                Connectez-vous à l'espace admin pour examiner le dossier et le diplôme joint.

                — SprachReise
                """,
                application.getId(),
                nullSafe(application.getFirstName()),
                nullSafe(application.getLastName()),
                nullSafe(application.getEmail()),
                nullSafe(application.getPhone()),
                String.valueOf(application.getRequestedLevelId()),
                nullSafe(application.getNativeLanguage()),
                nullSafe(application.getMotivation())
        );
        send(adminEmail, subject, body);
    }

    public void sendWelcomeTrainer(String email, String tempPassword) {
        String subject = "Bienvenue formateur SprachReise — votre compte est prêt";
        String body = String.format("""
                Bonjour,

                Félicitations ! Votre candidature de formateur sur SprachReise a été acceptée.

                Voici vos identifiants de connexion :
                  Email                  : %s
                  Mot de passe temporaire: %s

                Veuillez modifier votre mot de passe à votre première connexion depuis l'application mobile.

                Bienvenue dans la communauté SprachReise — Lernen ohne Grenzen.

                — L'équipe SprachReise
                """, email, tempPassword);
        send(email, subject, body);
    }

    public void sendRejection(String email, String motif) {
        String subject = "Suite à votre candidature formateur SprachReise";
        String body = String.format("""
                Bonjour,

                Nous vous remercions pour l'intérêt que vous portez à SprachReise.

                Après examen de votre dossier, nous ne sommes pas en mesure de donner suite à votre candidature pour le moment.

                Motif :
                %s

                Vous pouvez soumettre une nouvelle candidature ultérieurement si votre situation évolue.

                — L'équipe SprachReise
                """, motif);
        send(email, subject, body);
    }

    public void notifyExamSubmission(String trainerEmail, String learnerName, String examTitle) {
        String subject = "Nouvelle copie reçue pour l'épreuve « " + examTitle + " »";
        String body = String.format("""
                Bonjour,

                L'apprenant %s vient de soumettre une copie pour votre épreuve « %s ».

                Connectez-vous à SprachReise pour la corriger.

                — SprachReise
                """, learnerName, examTitle);
        send(trainerEmail, subject, body);
    }

    public void notifyExamGraded(String learnerEmail, String examTitle,
                                 java.math.BigDecimal grade, String feedback) {
        String subject = "Votre copie « " + examTitle + " » a été corrigée";
        String body = String.format("""
                Bonjour,

                Votre épreuve « %s » a été corrigée par votre formateur.

                Note     : %s/20
                Feedback :
                %s

                Connectez-vous à SprachReise pour consulter le détail.

                — SprachReise
                """, examTitle, grade, nullSafe(feedback));
        send(learnerEmail, subject, body);
    }

    public void notifyLearnersSessionScheduled(Long levelId, String title,
                                               java.time.LocalDateTime scheduledStart) {
        // Pour le MVP, on log seulement — un vrai envoi nécessiterait de boucler
        // sur les apprenants du niveau. La logique de fan-out sera ajoutée plus tard.
        log.info("===== PUSH/EMAIL [LEARNERS level_id={}] : LIVE SESSION SCHEDULED =====", levelId);
        log.info("Titre     : {}", title);
        log.info("Niveau    : level_id={}", levelId);
        log.info("Date      : {}", scheduledStart);
        log.info("======================================================================");
    }

    public void sendInvitation(String email, String token, String template) {
        String link = "http://localhost:8090/api/trainer-applications/from-invitation?token=" + token;
        boolean isDe = "DE".equalsIgnoreCase(template);
        String subject;
        String body;
        if (isDe) {
            subject = "Einladung als Trainer auf SprachReise";
            body = String.format("""
                    Hallo,

                    Sie wurden eingeladen, der SprachReise-Plattform als Deutschtrainer beizutreten.

                    Bitte klicken Sie auf den folgenden Link, um Ihre Bewerbung einzureichen :
                    %s

                    Dieser Link ist 7 Tage lang gültig.

                    — SprachReise
                    """, link);
        } else {
            subject = "Invitation à rejoindre SprachReise comme formateur";
            body = String.format("""
                    Bonjour,

                    Vous êtes invité(e) à rejoindre la plateforme SprachReise en tant que formateur d'allemand.

                    Cliquez sur le lien suivant pour soumettre votre candidature :
                    %s

                    Ce lien est valide pendant 7 jours.

                    — L'équipe SprachReise
                    """, link);
        }
        send(email, subject, body);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
