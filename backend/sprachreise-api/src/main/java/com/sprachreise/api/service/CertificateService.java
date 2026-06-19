package com.sprachreise.api.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.sprachreise.api.entity.Certificate;
import com.sprachreise.api.entity.LearnerProgress;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.CertificateRepository;
import com.sprachreise.api.repository.LearnerProgressRepository;
import com.sprachreise.api.repository.TrainerProfileRepository;
import com.sprachreise.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CertificateService {

    private static final DeviceRgb GOLD   = new DeviceRgb(184, 137, 58);
    private static final DeviceRgb DEEP   = new DeviceRgb(55, 38, 25);
    private static final DeviceRgb CREAM  = new DeviceRgb(249, 244, 232);
    private static final DeviceRgb MUTED  = new DeviceRgb(174, 145, 130);

    private final CertificateRepository    certRepo;
    private final LearnerProgressRepository progressRepo;
    private final UserRepository           userRepo;
    private final TrainerProfileRepository trainerRepo;
    private final JavaMailSender           mailSender;

    @Value("${storage.upload-dir}")
    private String storageDir;

    @Value("${spring.mail.username}")
    private String mailFrom;

    public CertificateService(CertificateRepository certRepo,
                               LearnerProgressRepository progressRepo,
                               UserRepository userRepo,
                               TrainerProfileRepository trainerRepo,
                               JavaMailSender mailSender) {
        this.certRepo    = certRepo;
        this.progressRepo = progressRepo;
        this.userRepo    = userRepo;
        this.trainerRepo = trainerRepo;
        this.mailSender  = mailSender;
    }

    // ── Apprenants éligibles (completion >= 70%, pas encore certifiés) ────────
    public List<Map<String, Object>> getEligibleLearners() {
        List<LearnerProgress> all = progressRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (LearnerProgress lp : all) {
            if (Boolean.TRUE.equals(lp.getCertified())) continue;
            if (lp.getCompletionPercentage() == null) continue;
            if (lp.getCompletionPercentage().compareTo(new BigDecimal("70")) < 0) continue;

            userRepo.findById(lp.getLearnerId()).ifPresent(learner -> {
                boolean alreadyCertified = certRepo.existsByLearnerIdAndLevelCodeAndRevokedFalse(
                    learner.getId(), learner.getLevelCode() != null ? learner.getLevelCode() : "");

                if (!alreadyCertified) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("learnerId", learner.getId());
                    m.put("learnerName", learner.getFirstName() + " " + learner.getLastName());
                    m.put("email", learner.getEmail());
                    m.put("levelCode", learner.getLevelCode());
                    m.put("completion", lp.getCompletionPercentage());
                    m.put("qcmScore", lp.getQcmAvgScore());
                    m.put("coursesCompleted", lp.getCoursesCompleted());
                    m.put("sessionsAttended", lp.getSessionsAttended());
                    result.add(m);
                }
            });
        }
        return result;
    }

    // ── Émettre un certificat ─────────────────────────────────────────────────
    public Certificate emit(Long learnerId, Long adminId) throws Exception {
        User learner = userRepo.findById(learnerId)
            .orElseThrow(() -> new RuntimeException("Apprenant introuvable"));

        String levelCode = learner.getLevelCode() != null ? learner.getLevelCode() : "A1";

        if (certRepo.existsByLearnerIdAndLevelCodeAndRevokedFalse(learnerId, levelCode)) {
            throw new RuntimeException("Un certificat actif existe déjà pour ce niveau");
        }

        // Trouver le formateur assigné
        Long trainerId = learner.getAssignedTrainerId();

        // Numéro unique : SR-2026-B1-000042
        String year = String.valueOf(LocalDateTime.now().getYear());
        long count  = certRepo.count() + 1;
        String certNumber = String.format("SR-%s-%s-%06d", year, levelCode, count);

        // Générer le PDF
        String pdfRelPath = "diplomas/cert_" + certNumber.replace("-", "_") + ".pdf";
        File pdfFile = new File(storageDir + "/" + pdfRelPath);
        pdfFile.getParentFile().mkdirs();

        byte[] pdfBytes = generatePdf(learner, levelCode, certNumber, trainerId);
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            fos.write(pdfBytes);
        }

        // SHA-256 du PDF
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(pdfBytes);
        StringBuilder sha = new StringBuilder();
        for (byte b : hashBytes) sha.append(String.format("%02x", b));

        // Sauvegarder en BDD
        Certificate cert = new Certificate();
        cert.setLearnerId(learnerId);
        cert.setLevelCode(levelCode);
        cert.setCertificateNumber(certNumber);
        cert.setPdfPath(pdfRelPath);
        cert.setPdfSha256(sha.toString());
        cert.setTrainerId(trainerId);
        cert.setIssuedBy(adminId);
        cert.setIssuedAt(LocalDateTime.now());
        cert.setRevoked(false);
        certRepo.save(cert);

        // Marquer comme certifié dans learner_progress
        progressRepo.findByLearnerIdAndLevelId(learnerId, null).ifPresent(lp -> {
            lp.setCertified(true);
            lp.setCertifiedAt(LocalDateTime.now());
            progressRepo.save(lp);
        });

        // Envoyer l'email en arrière-plan
        sendCertificateEmail(learner, cert, pdfBytes);

        return cert;
    }

    // ── Révoquer un certificat ────────────────────────────────────────────────
    public void revoke(Long certId, Long adminId, String reason) {
        Certificate cert = certRepo.findById(certId)
            .orElseThrow(() -> new RuntimeException("Certificat introuvable"));
        cert.setRevoked(true);
        cert.setRevokedAt(LocalDateTime.now());
        cert.setRevokedBy(adminId);
        cert.setRevokeReason(reason);
        certRepo.save(cert);
    }

    // ── Certificats d'un apprenant ────────────────────────────────────────────
    public List<Certificate> getByLearner(Long learnerId) {
        return certRepo.findAllByLearnerIdOrderByIssuedAtDesc(learnerId);
    }

    // ── Générer le PDF avec iText7 ────────────────────────────────────────────
    private byte[] generatePdf(User learner, String levelCode, String certNumber, Long trainerId) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf  = new PdfDocument(writer);
        pdf.setDefaultPageSize(PageSize.A4.rotate()); // Paysage

        // Métadonnées
        PdfDocumentInfo info = pdf.getDocumentInfo();
        info.setTitle("Certificat SprachReise – " + certNumber);
        info.setAuthor("SprachReise");
        info.setSubject("Certificat de niveau " + levelCode + " en allemand");

        Document doc = new Document(pdf);
        doc.setMargins(40, 50, 40, 50);

        PdfFont fontSerif  = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
        PdfFont fontBold   = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
        PdfFont fontItalic = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);
        PdfFont fontMono   = PdfFontFactory.createFont(StandardFonts.COURIER);

        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
        String learnerName = learner.getFirstName() + " " + learner.getLastName().toUpperCase();

        String trainerName = "SprachReise";
        if (trainerId != null) {
            trainerName = userRepo.findById(trainerId)
                .map(t -> t.getFirstName() + " " + t.getLastName())
                .orElse("SprachReise");
        }

        String levelLabel = getLevelLabel(levelCode);

        // ── Entête ─────────────────────────────────────────────────────────────
        doc.add(new Paragraph()
            .add(new Text("✦  SprachReise  ✦").setFont(fontSerif).setFontSize(11).setFontColor(GOLD))
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(4));

        // Ligne décorative
        doc.add(new LineSeparator(new SolidLine(1f)).setStrokeColor(GOLD).setMarginBottom(12));

        // Titre
        doc.add(new Paragraph("CERTIFICAT DE COMPÉTENCE")
            .setFont(fontBold)
            .setFontSize(28)
            .setFontColor(DEEP)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(4));

        doc.add(new Paragraph("EN LANGUE ALLEMANDE")
            .setFont(fontItalic)
            .setFontSize(14)
            .setFontColor(MUTED)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // Texte principal
        doc.add(new Paragraph("La plateforme SprachReise certifie que")
            .setFont(fontSerif)
            .setFontSize(13)
            .setFontColor(DEEP)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(8));

        // Nom de l'apprenant
        doc.add(new Paragraph(learnerName)
            .setFont(fontBold)
            .setFontSize(30)
            .setFontColor(GOLD)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(8));

        doc.add(new Paragraph("a validé avec succès le niveau")
            .setFont(fontSerif)
            .setFontSize(13)
            .setFontColor(DEEP)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10));

        // Niveau
        doc.add(new Paragraph(levelCode + "  —  " + levelLabel)
            .setFont(fontBold)
            .setFontSize(22)
            .setFontColor(DEEP)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(6));

        doc.add(new Paragraph("du Cadre Européen Commun de Référence pour les Langues (CECRL)")
            .setFont(fontItalic)
            .setFontSize(11)
            .setFontColor(MUTED)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // Ligne décorative
        doc.add(new LineSeparator(new SolidLine(0.5f)).setStrokeColor(GOLD).setMarginBottom(16));

        // Infos bas de page sur 3 colonnes
        Table footTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
            .useAllAvailableWidth()
            .setBorder(Border.NO_BORDER)
            .setMarginBottom(14);

        // Colonne gauche : date
        Cell colLeft = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.LEFT);
        colLeft.add(new Paragraph("Date d'émission").setFont(fontBold).setFontSize(9).setFontColor(MUTED));
        colLeft.add(new Paragraph(dateStr).setFont(fontSerif).setFontSize(11).setFontColor(DEEP));
        footTable.addCell(colLeft);

        // Colonne centre : email
        Cell colCenter = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
        colCenter.add(new Paragraph("Apprenant").setFont(fontBold).setFontSize(9).setFontColor(MUTED));
        colCenter.add(new Paragraph(learner.getEmail()).setFont(fontSerif).setFontSize(10).setFontColor(DEEP));
        if (learner.getCity() != null) {
            colCenter.add(new Paragraph(learner.getCity()).setFont(fontItalic).setFontSize(10).setFontColor(MUTED));
        }
        footTable.addCell(colCenter);

        // Colonne droite : formateur
        Cell colRight = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        colRight.add(new Paragraph("Formateur référent").setFont(fontBold).setFontSize(9).setFontColor(MUTED));
        colRight.add(new Paragraph(trainerName).setFont(fontSerif).setFontSize(11).setFontColor(DEEP));
        footTable.addCell(colRight);

        doc.add(footTable);

        // Ligne décorative
        doc.add(new LineSeparator(new SolidLine(0.5f)).setStrokeColor(GOLD).setMarginBottom(8));

        // Numéro et SHA256
        doc.add(new Paragraph("N° " + certNumber)
            .setFont(fontMono)
            .setFontSize(9)
            .setFontColor(MUTED)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(2));

        doc.add(new Paragraph("Lernen · Reisen · Entdecken  —  sprachreise.app")
            .setFont(fontItalic)
            .setFontSize(9)
            .setFontColor(GOLD)
            .setTextAlignment(TextAlignment.CENTER));

        doc.close();
        return baos.toByteArray();
    }

    // ── Envoyer l'email avec le PDF en pièce jointe ───────────────────────────
    @Async
    public void sendCertificateEmail(User learner, Certificate cert, byte[] pdfBytes) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(learner.getEmail());
            helper.setSubject("🎓 Votre certificat SprachReise – Niveau " + cert.getLevelCode());

            String levelLabel = getLevelLabel(cert.getLevelCode());
            String html = buildEmailHtml(learner, cert, levelLabel);
            helper.setText(html, true);

            // PDF en pièce jointe
            helper.addAttachment(
                "Certificat_SprachReise_" + cert.getLevelCode() + "_" + learner.getLastName().toUpperCase() + ".pdf",
                new org.springframework.core.io.ByteArrayResource(pdfBytes),
                "application/pdf"
            );

            mailSender.send(msg);
        } catch (Exception e) {
            // Log mais ne bloque pas
            System.err.println("Erreur envoi email certificat : " + e.getMessage());
        }
    }

    private String buildEmailHtml(User learner, Certificate cert, String levelLabel) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:'Georgia',serif;background:#F9F4E8;margin:0;padding:0;">
            <div style="max-width:600px;margin:32px auto;background:#fff;border:2px solid #B8893A;border-radius:8px;overflow:hidden;">
              <div style="background:#372619;padding:28px 32px;text-align:center;">
                <p style="color:#B8893A;font-size:13px;letter-spacing:4px;margin:0 0 8px;">SPRACHREISE</p>
                <h1 style="color:#F9F4E8;font-size:26px;margin:0;font-style:italic;">Félicitations !</h1>
              </div>
              <div style="padding:32px;">
                <p style="color:#372619;font-size:15px;">Bonjour <strong>%s</strong>,</p>
                <p style="color:#372619;font-size:15px;line-height:1.6;">
                  Nous sommes ravis de vous informer que vous avez <strong>validé le niveau
                  <span style="color:#B8893A;">%s — %s</span></strong>
                  en langue allemande sur la plateforme SprachReise.
                </p>
                <div style="background:#F9F4E8;border-left:4px solid #B8893A;padding:16px 20px;margin:20px 0;border-radius:0 6px 6px 0;">
                  <p style="margin:0 0 6px;color:#AE9182;font-size:11px;letter-spacing:2px;">CERTIFICAT</p>
                  <p style="margin:0;color:#372619;font-size:18px;font-weight:bold;">%s</p>
                  <p style="margin:4px 0 0;color:#AE9182;font-size:12px;">Émis le %s</p>
                </div>
                <p style="color:#372619;font-size:14px;line-height:1.6;">
                  Votre certificat PDF est joint à cet email. Vous pouvez également le télécharger
                  depuis votre profil dans l'application.
                </p>
                <p style="color:#AE9182;font-size:12px;font-style:italic;">
                  Continuez votre voyage linguistique — le prochain niveau vous attend !
                </p>
              </div>
              <div style="background:#F5EFE3;padding:16px 32px;text-align:center;border-top:1px solid #D9CAAA;">
                <p style="color:#AE9182;font-size:11px;margin:0;font-style:italic;">
                  Lernen · Reisen · Entdecken — sprachreise.app
                </p>
              </div>
            </div>
            </body>
            </html>
            """.formatted(
                learner.getFirstName(),
                cert.getLevelCode(), levelLabel,
                cert.getCertificateNumber(),
                cert.getIssuedAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH))
            );
    }

    private String getLevelLabel(String code) {
        return switch (code != null ? code : "") {
            case "A1" -> "Découverte";
            case "A2" -> "Survie";
            case "B1" -> "Seuil";
            case "B2" -> "Avancé";
            case "C1" -> "Autonome";
            case "C2" -> "Maîtrise";
            default   -> "";
        };
    }
}
