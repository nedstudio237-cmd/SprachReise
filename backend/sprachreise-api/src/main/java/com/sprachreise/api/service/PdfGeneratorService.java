package com.sprachreise.api.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.sprachreise.api.entity.Course;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class PdfGeneratorService {

    @Value("${storage.upload-dir}")
    private String storageDir;

    // Colors
    private static final DeviceRgb C_DEEP    = new DeviceRgb(55, 38, 25);
    private static final DeviceRgb C_ACCENT  = new DeviceRgb(161, 94, 45);
    private static final DeviceRgb C_GOLD    = new DeviceRgb(184, 137, 58);
    private static final DeviceRgb C_PARCH   = new DeviceRgb(249, 244, 232);
    private static final DeviceRgb C_CREAM   = new DeviceRgb(217, 202, 170);
    private static final DeviceRgb C_MUTED   = new DeviceRgb(174, 145, 130);
    private static final DeviceRgb C_WHITE   = new DeviceRgb(255, 255, 255);

    // Level badge colors
    private static final Map<Long, DeviceRgb> LEVEL_COLORS = new HashMap<>() {{
        put(1L, new DeviceRgb(16, 185, 129));
        put(2L, new DeviceRgb(59, 130, 246));
        put(3L, new DeviceRgb(139, 92, 246));
        put(4L, new DeviceRgb(245, 158, 11));
        put(5L, new DeviceRgb(236, 72, 153));
        put(6L, new DeviceRgb(239, 68, 68));
    }};

    private static final Map<Long, String> LEVEL_CODES = new HashMap<>() {{
        put(1L,"A1"); put(2L,"A2"); put(3L,"B1");
        put(4L,"B2"); put(5L,"C1"); put(6L,"C2");
    }};

    // Vocabulary per theme: {german, type, french, example sentence}
    private static final Map<String, String[][]> THEME_VOCAB = new HashMap<>() {{
        put("Phonétique", new String[][]{
            {"das Alphabet, -e","n.","l'alphabet","Das Alphabet hat 26 Buchstaben plus Umlaute."},
            {"der Buchstabe, -n","n.","la lettre","A ist der erste Buchstabe."},
            {"der Vokal, -e","n.","la voyelle","a, e, i, o, u sind Vokale."},
            {"der Umlaut, -e","n.","l'umlaut (voyelle modifiée)","ä, ö, ü sind die deutschen Umlaute."},
            {"die Aussprache","n.","la prononciation","Die Aussprache des ß klingt wie ss."},
            {"das Eszett / ß","n.","l'eszett","Straße — le ß remplace ss après une voyelle longue."},
        });
        put("Communication", new String[][]{
            {"Guten Tag!","expr.","Bonjour !","Guten Tag, wie geht es Ihnen?"},
            {"Ich heiße...","expr.","Je m'appelle...","Ich heiße Marie. Und Sie?"},
            {"Wie heißen Sie?","expr.","Comment vous appelez-vous ?","Entschuldigung, wie heißen Sie?"},
            {"Bitte / Danke","expr.","S'il vous plaît / Merci","Danke! — Bitte sehr!"},
            {"Auf Wiedersehen","expr.","Au revoir","Auf Wiedersehen, bis morgen!"},
            {"Entschuldigung","expr.","Excusez-moi / Pardon","Entschuldigung, wo ist der Bahnhof?"},
        });
        put("Vocabulaire", new String[][]{
            {"das Wort, -̈er","n.","le mot","Dieses Wort kenne ich nicht."},
            {"die Bedeutung, -en","n.","la signification","Was ist die Bedeutung von diesem Wort?"},
            {"lernen","v.","apprendre","Ich lerne Deutsch seit zwei Monaten."},
            {"verstehen","v.","comprendre","Ich verstehe das nicht. Können Sie langsamer sprechen?"},
            {"wiederholen","v.","répéter","Können Sie das bitte wiederholen?"},
            {"übersetzen","v.","traduire","Wie übersetzt man das ins Französische?"},
        });
        put("Grammaire", new String[][]{
            {"der Artikel","n.","l'article","der (masc.), die (fém./pl.), das (neutre)"},
            {"das Verb, -en","n.","le verbe","sein, haben, werden sont des verbes auxiliaires."},
            {"die Konjugation","n.","la conjugaison","Ich lerne, du lernst, er/sie lernt..."},
            {"der Kasus","n.","le cas grammatical","Nominatif, Accusatif, Datif, Génitif"},
            {"das Adjektiv, -e","n.","l'adjectif","Das Buch ist interessant."},
            {"die Präposition, -en","n.","la préposition","in, an, auf, vor, nach, mit, von..."},
        });
        put("Vie quotidienne", new String[][]{
            {"essen / aß / gegessen","v.","manger","Ich esse gern Brot zum Frühstück."},
            {"trinken","v.","boire","Was möchten Sie trinken?"},
            {"schlafen","v.","dormir","Ich schlafe acht Stunden pro Nacht."},
            {"arbeiten","v.","travailler","Er arbeitet in einem Büro."},
            {"einkaufen","v.","faire les courses","Ich gehe samstags einkaufen."},
            {"frühstücken","v.","prendre le petit-déjeuner","Was frühstückst du?"},
        });
        put("Communication professionnelle", new String[][]{
            {"die Bewerbung, -en","n.","la candidature","Ich habe eine Bewerbung geschrieben."},
            {"das Vorstellungsgespräch","n.","l'entretien d'embauche","Das Vorstellungsgespräch war sehr gut."},
            {"der Lebenslauf, -̈e","n.","le CV","Schicken Sie mir bitte Ihren Lebenslauf."},
            {"das Gehalt, -̈er","n.","le salaire","Was sind Ihre Gehaltsvorstellungen?"},
            {"die Kündigung, -en","n.","le licenciement / la démission","Er hat seine Kündigung eingereicht."},
            {"die Stelle, -n","n.","le poste","Die Stelle ist ab sofort zu besetzen."},
        });
        put("Société", new String[][]{
            {"die Meinung, -en","n.","l'opinion","Meiner Meinung nach ist das falsch."},
            {"einerseits... andererseits","conn.","d'un côté... de l'autre","Einerseits ist es teuer, andererseits praktisch."},
            {"obwohl","conj.","bien que","Obwohl es schwer ist, lerne ich weiter."},
            {"deshalb","adv.","c'est pourquoi","Er ist krank, deshalb bleibt er zu Hause."},
            {"die Gesellschaft, -en","n.","la société","Die Gesellschaft verändert sich ständig."},
            {"das Thema, Themen","n.","le sujet / thème","Das ist ein wichtiges Thema."},
        });
        put("Économie", new String[][]{
            {"das Unternehmen, -","n.","l'entreprise","Das Unternehmen hat 500 Mitarbeiter."},
            {"investieren","v.","investir","Man sollte in Bildung investieren."},
            {"der Gewinn, -e","n.","le bénéfice","Der Gewinn ist dieses Jahr gestiegen."},
            {"verhandeln","v.","négocier","Wir müssen noch über den Preis verhandeln."},
            {"die Inflation","n.","l'inflation","Die Inflation steigt in ganz Europa."},
            {"der Markt, -̈e","n.","le marché","Der Markt ist sehr wettbewerbsintensiv."},
        });
        put("Culture", new String[][]{
            {"die Kultur, -en","n.","la culture","Die deutsche Kultur ist sehr reich."},
            {"das Werk, -e","n.","l'œuvre","Das ist ein Werk von Goethe."},
            {"der Künstler, -","n.","l'artiste","Der Künstler lebt in Berlin."},
            {"analysieren","v.","analyser","Wir analysieren diesen Roman."},
            {"vergleichen","v.","comparer","Vergleichen Sie die beiden Texte."},
            {"der Einfluss, -̈e","n.","l'influence","Der Einfluss der Romantik ist groß."},
        });
        put("Académique", new String[][]{
            {"die These, -n","n.","la thèse","Die These wird am Schluss bewiesen."},
            {"belegen / beweisen","v.","démontrer / prouver","Sie müssen Ihre These mit Fakten belegen."},
            {"das Fazit, -s","n.","la conclusion","Im Fazit fasst man die Ergebnisse zusammen."},
            {"erörtern","v.","analyser / discuter","Erörtern Sie die Vor- und Nachteile."},
            {"die Studie, -n","n.","l'étude","Laut einer Studie der Universität..."},
            {"hypothetisch","adj.","hypothétique","Das ist nur eine hypothetische Annahme."},
        });
        put("Littérature", new String[][]{
            {"die Metapher, -n","n.","la métaphore","Die Metapher verleiht dem Text Tiefe."},
            {"die Erzählung, -en","n.","le récit / la narration","Der Ich-Erzähler schildert seine Erlebnisse."},
            {"interpretieren","v.","interpréter","Wie interpretieren Sie diesen Satz?"},
            {"symbolisch","adj.","symbolique","Das Feuer hat eine symbolische Bedeutung."},
            {"die Figur, -en","n.","le personnage","Die Hauptfigur ist ein Reisender."},
            {"das Motiv, -e","n.","le motif / thème récurrent","Das Motiv der Einsamkeit zieht sich durch den Roman."},
        });
        put("Philosophie", new String[][]{
            {"das Denken","n.","la pensée","Kritisches Denken ist eine wichtige Fähigkeit."},
            {"hinterfragen","v.","remettre en question","Man sollte alles kritisch hinterfragen."},
            {"die Erkenntnis, -se","n.","la connaissance / la prise de conscience","Das ist eine wichtige Erkenntnis."},
            {"der Diskurs, -e","n.","le discours","Habermas entwickelt die Diskurstheorie."},
            {"dialektisch","adj.","dialectique","Die dialektische Methode nach Hegel."},
            {"das Wesen","n.","l'essence / la nature","Was ist das Wesen des Menschen?"},
        });
        put("Stylistique", new String[][]{
            {"der Stil, -e","n.","le style","Sein Schreibstil ist sehr präzise."},
            {"nuanciert","adj.","nuancé","Eine nuancierte Antwort ist gefragt."},
            {"das Register, -","n.","le registre de langue","Formell oder informell — das Register zählt."},
            {"der Ausdruck, -̈e","n.","l'expression","Das ist ein treffender Ausdruck."},
            {"das Sprachgefühl","n.","le sens de la langue","Ein gutes Sprachgefühl braucht Zeit."},
            {"umgangssprachlich","adj.","familier / courant","Das klingt zu umgangssprachlich."},
        });
        put("Certification", new String[][]{
            {"das Zertifikat, -e","n.","le certificat","Das Zertifikat wird weltweit anerkannt."},
            {"beherrschen","v.","maîtriser","Sie beherrscht die Sprache perfekt."},
            {"muttersprachlich","adj.","maternel / natif","Sein Deutsch klingt fast muttersprachlich."},
            {"flüssig","adj.","fluide / couramment","Sie spricht Deutsch flüssig."},
            {"die Prüfung, -en","n.","l'examen","Die Goethe-Prüfung ist international anerkannt."},
            {"das Niveau, -s","n.","le niveau","Welches Sprachniveau haben Sie?"},
        });
        put("Expression orale", new String[][]{
            {"spontan","adj.","spontané","Sprechen Sie spontan — ohne nachzudenken."},
            {"der Dialekt, -e","n.","le dialecte","Das Bayerische ist ein bekannter Dialekt."},
            {"authentisch","adj.","authentique","Klingt das authentisch für Sie?"},
            {"der Ausdruck, -̈e","n.","l'expression","Dieser Ausdruck klingt sehr natürlich."},
            {"das Gespräch, -e","n.","la conversation","Das Gespräch verlief sehr flüssig."},
            {"der Akzent, -e","n.","l'accent","Ein leichter Akzent ist völlig normal."},
        });
        put("Communication avancée", new String[][]{
            {"überzeugen","v.","convaincre","Wie kann man jemanden überzeugen?"},
            {"das Argument, -e","n.","l'argument","Das ist ein starkes Argument."},
            {"widersprechen","v.","contredire","Ich muss Ihnen da widersprechen."},
            {"das Protokoll, -e","n.","le procès-verbal","Wer schreibt das Protokoll?"},
            {"die Verhandlung, -en","n.","la négociation","Die Verhandlung war erfolgreich."},
            {"kompromissbereit","adj.","prêt à faire des compromis","Seien Sie kompromissbereit!"},
        });
        put("Éducation", new String[][]{
            {"die Universität, -en","n.","l'université","Er studiert an der Universität Berlin."},
            {"das Abitur","n.","le baccalauréat (allemand)","Das Abitur ist der deutsche Schulabschluss."},
            {"das Stipendium, -ien","n.","la bourse (d'études)","Sie hat ein DAAD-Stipendium bekommen."},
            {"der Abschluss, -̈e","n.","le diplôme / la fin d'études","Welchen Abschluss haben Sie?"},
            {"die Prüfung, -en","n.","l'examen","Die Prüfung findet nächste Woche statt."},
            {"das Semester, -","n.","le semestre","Im dritten Semester lernt man..."},
        });
    }};

    // Grammar tip per level
    private static final Map<Long, String> LEVEL_GRAMMAR = new HashMap<>() {{
        put(1L, "ARTICLES DÉFINIS & INDÉFINIS\n"
            + "• Masculin : der Mann (déf.) / ein Mann (indéf.)\n"
            + "• Féminin : die Frau / eine Frau\n"
            + "• Neutre : das Kind / ein Kind\n"
            + "• Pluriel : die Kinder (pas d'article indéfini au pluriel)\n"
            + "Astuce : Apprenez toujours le mot avec son article !");
        put(2L, "LE PASSÉ COMPOSÉ (PERFEKT)\n"
            + "• Formation : haben/sein + participe passé (Partizip II)\n"
            + "• Avec haben : Ich habe gegessen. (J'ai mangé)\n"
            + "• Avec sein : Ich bin gegangen. (Je suis allé)\n"
            + "• Les verbes de mouvement et de changement d'état → sein\n"
            + "• Partizip II régulier : ge- + radical + -t → gemacht, gespielt");
        put(3L, "LE SUBJONCTIF II (KONJUNKTIV II)\n"
            + "• Exprime des hypothèses, souhaits, politesse\n"
            + "• Formation : Konjunktiv II de sein = wäre, de haben = hätte\n"
            + "• würde + infinitif pour les autres verbes\n"
            + "• Exemples : Ich würde gern reisen. / Wenn ich reich wäre...\n"
            + "• Propositions subordonnées : weil, dass, obwohl, wenn (verbe en fin)");
        put(4L, "LA VOIX PASSIVE (PASSIV)\n"
            + "• Passif d'action : werden + Partizip II\n"
            + "• Ex. : Das Buch wird gelesen. (Le livre est lu.)\n"
            + "• Passif d'état : sein + Partizip II\n"
            + "• Ex. : Das Buch ist gelesen. (Le livre a été lu.)\n"
            + "• Propositions relatives au génitif : das Buch, dessen Autor...");
        put(5L, "NOMINALISATIONS & REGISTRES\n"
            + "• Nominalisation : nominalisiertes Verb = Großschreibung\n"
            + "• Das Schreiben / das Lernen / das Denken\n"
            + "• Registres : formell / umgangssprachlich / wissenschaftlich\n"
            + "• Infinitives : um...zu (pour), ohne...zu (sans), anstatt...zu (au lieu de)\n"
            + "• Genitiv : des Mannes, der Frau, des Kindes, der Kinder");
        put(6L, "MAÎTRISE TOTALE DE LA LANGUE\n"
            + "• Tous les temps : Präsens, Präteritum, Perfekt, Plusquamperfekt, Futur I & II\n"
            + "• Konjunktiv I (style indirect) : Er sagte, er sei krank.\n"
            + "• Participiales : Das von mir geschriebene Buch...\n"
            + "• Faux amis : also (donc ≠ aussi), bekommen (recevoir ≠ devenir)\n"
            + "• Registre académique : des Weiteren, im Hinblick auf, hinsichtlich");
    }};

    public String generateCoursePdf(Course course) throws IOException {
        String pdfDir = storageDir + "/courses/pdf";
        new File(pdfDir).mkdirs();
        String filename = "course_" + course.getId() + ".pdf";
        String fullPath = pdfDir + "/" + filename;

        PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA, PdfEncodings.CP1252,
                PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD, PdfEncodings.CP1252,
                PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
        PdfFont fontOblique = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE, PdfEncodings.CP1252,
                PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);

        PdfDocument pdf = new PdfDocument(new PdfWriter(fullPath));
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(0, 40, 40, 40);

        Long levelId = course.getLevelId() != null ? course.getLevelId() : 1L;
        String levelCode = LEVEL_CODES.getOrDefault(levelId, "A1");
        DeviceRgb levelColor = LEVEL_COLORS.getOrDefault(levelId, C_ACCENT);

        // ── HEADER BAR ──
        Table header = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth()
                .setMarginLeft(-40).setMarginRight(-40)
                .setMarginBottom(0);

        Cell logoCell = new Cell().setBorder(null)
                .setBackgroundColor(C_DEEP)
                .setPadding(18).setPaddingLeft(40);
        logoCell.add(new Paragraph("SprachReise").setFont(fontBold).setFontSize(20).setFontColor(C_PARCH).setMargin(0));
        logoCell.add(new Paragraph("Guide de cours officiel").setFont(fontOblique).setFontSize(10).setFontColor(C_MUTED).setMargin(0));
        header.addCell(logoCell);

        Cell levelCell = new Cell().setBorder(null)
                .setBackgroundColor(levelColor)
                .setPadding(18).setPaddingRight(40)
                .setTextAlignment(TextAlignment.RIGHT);
        levelCell.add(new Paragraph(levelCode).setFont(fontBold).setFontSize(28).setFontColor(C_WHITE).setMargin(0));
        levelCell.add(new Paragraph("Niveau CECRL").setFont(fontRegular).setFontSize(9).setFontColor(C_WHITE).setMargin(0));
        header.addCell(levelCell);
        doc.add(header);

        doc.setLeftMargin(40);
        doc.setRightMargin(40);

        // ── COURSE TITLE & META ──
        doc.add(new Paragraph("\n"));
        if (course.getTheme() != null) {
            doc.add(new Paragraph(course.getTheme().toUpperCase())
                    .setFont(fontBold).setFontSize(9).setFontColor(C_GOLD)
                    .setCharacterSpacing(1.5f).setMarginBottom(4));
        }
        doc.add(new Paragraph(course.getTitle())
                .setFont(fontBold).setFontSize(22).setFontColor(C_DEEP)
                .setMarginBottom(8).setMultipliedLeading(1.2f));

        // Meta row
        String durationStr = course.getVideoDurationSec() != null
                ? Math.round(course.getVideoDurationSec() / 60.0) + " min"
                : "-";
        Table meta = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34})).useAllAvailableWidth();
        meta.addCell(metaCell("DUREE", durationStr, fontBold, fontRegular));
        meta.addCell(metaCell("THEME", course.getTheme() != null ? course.getTheme() : "-", fontBold, fontRegular));
        meta.addCell(metaCell("NIVEAU", levelCode, fontBold, fontRegular));
        doc.add(meta);

        doc.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(14).setMarginBottom(14)
                .setStrokeColor(C_GOLD));

        // ── DESCRIPTION ──
        sectionTitle(doc, "DESCRIPTION DU COURS", fontBold);
        String desc = course.getDescription() != null ? course.getDescription() : "";
        doc.add(new Paragraph(desc)
                .setFont(fontRegular).setFontSize(10.5f).setFontColor(C_DEEP)
                .setMultipliedLeading(1.6f).setMarginBottom(20));

        // ── VOCABULARY ──
        sectionTitle(doc, "VOCABULAIRE ESSENTIEL — " + levelCode, fontBold);
        String theme = course.getTheme() != null ? course.getTheme() : "Communication";
        String[][] vocab = THEME_VOCAB.getOrDefault(theme, THEME_VOCAB.get("Communication"));

        Table vocabTable = new Table(UnitValue.createPercentArray(new float[]{28, 8, 22, 42}))
                .useAllAvailableWidth().setMarginBottom(20);

        // Table header
        String[] vocabHeaders = {"Allemand", "Type", "Français", "Exemple"};
        for (String h : vocabHeaders) {
            vocabTable.addHeaderCell(new Cell().setBackgroundColor(C_DEEP)
                    .setBorder(new SolidBorder(C_ACCENT, 0.5f))
                    .setPadding(6)
                    .add(new Paragraph(h).setFont(fontBold).setFontSize(9).setFontColor(C_PARCH)));
        }
        boolean alt = false;
        for (String[] row : vocab) {
            DeviceRgb bg = alt ? new DeviceRgb(245, 239, 227) : C_WHITE;
            for (int i = 0; i < 4; i++) {
                String val = i < row.length ? row[i] : "";
                PdfFont f = (i == 0) ? fontBold : fontRegular;
                DeviceRgb fg = (i == 0) ? C_DEEP : new DeviceRgb(60, 40, 20);
                vocabTable.addCell(new Cell().setBackgroundColor(bg)
                        .setBorder(new SolidBorder(C_CREAM, 0.5f))
                        .setPadding(5)
                        .add(new Paragraph(val).setFont(f).setFontSize(9).setFontColor(fg)
                                .setMultipliedLeading(1.3f)));
            }
            alt = !alt;
        }
        doc.add(vocabTable);

        // ── GRAMMAR TIP ──
        String grammarTip = LEVEL_GRAMMAR.getOrDefault(levelId, "");
        if (!grammarTip.isEmpty()) {
            sectionTitle(doc, "POINT DE GRAMMAIRE — NIVEAU " + levelCode, fontBold);
            Table tipBox = new Table(1).useAllAvailableWidth().setMarginBottom(20);
            tipBox.addCell(new Cell().setBackgroundColor(new DeviceRgb(253, 246, 230))
                    .setBorderLeft(new SolidBorder(C_GOLD, 3))
                    .setBorderTop(new SolidBorder(C_CREAM, 0.5f))
                    .setBorderRight(new SolidBorder(C_CREAM, 0.5f))
                    .setBorderBottom(new SolidBorder(C_CREAM, 0.5f))
                    .setPadding(12)
                    .add(new Paragraph(grammarTip).setFont(fontRegular).setFontSize(9.5f)
                            .setFontColor(C_DEEP).setMultipliedLeading(1.6f)));
            doc.add(tipBox);
        }

        // ── FOOTER ──
        doc.add(new LineSeparator(new SolidLine(0.5f)).setStrokeColor(C_GOLD).setMarginBottom(8));
        Table footer = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        footer.addCell(new Cell().setBorder(null)
                .add(new Paragraph("© SprachReise — Cours " + levelCode + " · " + course.getTitle())
                        .setFont(fontRegular).setFontSize(8).setFontColor(C_MUTED)));
        footer.addCell(new Cell().setBorder(null).setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("sprachreise.app · Formation linguistique depuis l'Afrique")
                        .setFont(fontOblique).setFontSize(8).setFontColor(C_MUTED)));
        doc.add(footer);

        doc.close();
        return "courses/pdf/" + filename;
    }

    private void sectionTitle(Document doc, String text, PdfFont fontBold) {
        doc.add(new Paragraph(text)
                .setFont(fontBold).setFontSize(9).setFontColor(C_GOLD)
                .setCharacterSpacing(1f).setMarginBottom(8));
    }

    private Cell metaCell(String label, String value, PdfFont fontBold, PdfFont fontRegular) {
        Cell c = new Cell().setBorder(new SolidBorder(C_CREAM, 0.5f)).setPadding(8)
                .setBackgroundColor(new DeviceRgb(249, 244, 232));
        c.add(new Paragraph(label).setFont(fontBold).setFontSize(8)
                .setFontColor(C_MUTED).setCharacterSpacing(1).setMarginBottom(2));
        c.add(new Paragraph(value).setFont(fontBold).setFontSize(13).setFontColor(C_DEEP));
        return c;
    }
}
