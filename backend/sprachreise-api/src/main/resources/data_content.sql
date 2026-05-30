-- ============================================================
-- SprachReise — Contenu pédagogique complet A1 → C2
-- Programme aligné CECRL / Goethe-Institut
-- ============================================================

USE sprachreise;

-- Supprime l'ancien contenu demo pour le remplacer par du vrai contenu
DELETE FROM qcm_attempts;
DELETE FROM qcm_choices;
DELETE FROM qcm_questions;
DELETE FROM qcms;
DELETE FROM courses;

-- ============================================================
-- COURSES — 4 cours par niveau (24 au total)
-- trainer_id=2 à 6 (formateurs demo), level_id=1 à 6
-- ============================================================

-- ===== NIVEAU A1 (level_id = 1) =====
INSERT INTO courses (trainer_id, level_id, title, description, theme, video_path, video_duration_sec, pdf_path, status) VALUES
(2, 1, 'Alphabet et phonétique allemande',
 'Maîtrisez les 26 lettres + les umlauts (ä, ö, ü) et l''Eszett (ß). Apprenez la prononciation correcte de chaque son, les diphtongues (ei, au, eu) et les combinaisons consonantiques propres à l''allemand (sch, ch, sp, st). Exercices d''écoute et de répétition pour ancrer les automatismes.',
 'Phonétique', 'courses/a1/01_alphabet.mp4', 1800, 'courses/a1/01_alphabet.pdf', 'PUBLISHED'),

(3, 1, 'Se présenter et saluer',
 'Les formules de politesse essentielles : Guten Morgen, Guten Tag, Guten Abend, Gute Nacht, Auf Wiedersehen, Tschüss. Se présenter : Ich heiße..., Ich bin..., Ich komme aus..., Ich wohne in..., Ich bin ... Jahre alt. Questions de base : Wie heißen Sie? Woher kommen Sie? Wie alt sind Sie?',
 'Communication', 'courses/a1/02_vorstellung.mp4', 2100, 'courses/a1/02_vorstellung.pdf', 'PUBLISHED'),

(4, 1, 'La famille et les relations',
 'Vocabulaire de la famille : die Familie, der Vater, die Mutter, der Bruder, die Schwester, der Sohn, die Tochter, die Großeltern, der Onkel, die Tante. Les adjectifs possessifs mein/meine, dein/deine. Décrire sa famille : Mein Vater heißt..., Ich habe einen Bruder.',
 'Vocabulaire', 'courses/a1/03_familie.mp4', 1980, 'courses/a1/03_familie.pdf', 'PUBLISHED'),

(5, 1, 'Les chiffres, l''heure et les dates',
 'Chiffres 0-1000 : null, eins, zwei, drei... Heures : Es ist... Uhr, halb, Viertel nach, Viertel vor. Jours : Montag bis Sonntag. Mois : Januar bis Dezember. Dates : am ersten Januar, am zweiten März. Expressions : heute, morgen, gestern, übermorgen.',
 'Vocabulaire', 'courses/a1/04_zahlen_zeit.mp4', 2400, 'courses/a1/04_zahlen_zeit.pdf', 'PUBLISHED'),

(6, 1, 'Vie quotidienne et activités',
 'Verbes du quotidien : essen, trinken, schlafen, arbeiten, lernen, spielen, lesen, schreiben, kaufen, gehen. La conjugaison au présent (Präsens) des verbes réguliers et des irréguliers courants (sein, haben, werden). Phrases simples sur la routine : Ich stehe um 7 Uhr auf. Ich frühstücke.',
 'Grammaire', 'courses/a1/05_alltag.mp4', 2700, 'courses/a1/05_alltag.pdf', 'PUBLISHED'),

-- ===== NIVEAU A2 (level_id = 2) =====
(2, 2, 'Achats et commerce',
 'Vocabulaire du shopping : der Laden, der Supermarkt, die Bäckerei, der Markt. Demander le prix : Was kostet das? Wie viel kostet...? Expressions : Das ist zu teuer, Ich nehme das, Haben Sie...? Tailles et quantités. Monnaie : Euro und Cent. Comparer : billiger, teurer, größer.',
 'Communication', 'courses/a2/01_einkaufen.mp4', 2200, 'courses/a2/01_einkaufen.pdf', 'PUBLISHED'),

(3, 2, 'Transports et déplacements',
 'Moyens de transport : die U-Bahn, der Bus, der Zug, das Flugzeug, das Auto, das Fahrrad. Acheter un billet : Eine Fahrkarte nach..., Hin- und Rückfahrt, einfach. S''orienter : links, rechts, geradeaus, die erste Straße. Prépositions de lieu : in, an, auf, vor, hinter, neben, zwischen.',
 'Communication', 'courses/a2/02_transport.mp4', 2500, 'courses/a2/02_transport.pdf', 'PUBLISHED'),

(4, 2, 'Nourriture et restaurants',
 'Aliments : das Brot, die Milch, das Fleisch, das Gemüse, das Obst, der Fisch. Au restaurant : einen Tisch reservieren, die Speisekarte, bestellen, bezahlen. Exprimer ses goûts : Ich mag..., Ich esse gern..., Ich bin Vegetarier/Vegetarierin. Recettes simples : Zutaten, Zubereitung.',
 'Vocabulaire', 'courses/a2/03_essen.mp4', 1900, 'courses/a2/03_essen.pdf', 'PUBLISHED'),

(5, 2, 'Logement et description d''un lieu',
 'Types de logement : die Wohnung, das Haus, das Zimmer. Pièces : das Wohnzimmer, das Schlafzimmer, die Küche, das Badezimmer. Meubles : der Tisch, der Stuhl, das Bett, der Schrank. Louer : Ich suche eine Wohnung, Kaltmiete, Nebenkosten. Prépositions avec accusatif et datif.',
 'Vocabulaire', 'courses/a2/04_wohnen.mp4', 2300, 'courses/a2/04_wohnen.pdf', 'PUBLISHED'),

(6, 2, 'Santé et corps humain',
 'Parties du corps : der Kopf, die Hand, der Arm, das Bein, der Bauch, der Rücken. Chez le médecin : Ich habe Schmerzen, Ich bin krank, Mir ist schlecht. Maladies courantes : Erkältung, Grippe, Fieber, Kopfschmerzen. Médicaments : die Tablette, der Sirup. Impératif pour les instructions.',
 'Communication', 'courses/a2/05_gesundheit.mp4', 2100, 'courses/a2/05_gesundheit.pdf', 'PUBLISHED'),

-- ===== NIVEAU B1 (level_id = 3) =====
(2, 3, 'Le monde du travail',
 'Métiers et professions : der Arzt, der Lehrer, der Ingenieur, der Kaufmann. CV et lettre de motivation : Ausbildung, Berufserfahrung, Kenntnisse, Bewerbung. Entretien d''embauche : Stärken und Schwächen, Gehaltsverhandlung. Lieu de travail : das Büro, die Fabrik, home office. Konjunktiv II für höfliche Anfragen.',
 'Communication professionnelle', 'courses/b1/01_arbeit.mp4', 3000, 'courses/b1/01_arbeit.pdf', 'PUBLISHED'),

(3, 3, 'Voyages et tourisme',
 'Planifier un voyage : das Hotel, die Unterkunft, der Reiseführer, reservieren. À l''hôtel : einchecken, auschecken, das Einzelzimmer, das Doppelzimmer. Pays germanophones : Deutschland, Österreich, die Schweiz — culture et particularités. Réclamations : Ich möchte mich beschweren. Passé composé (Perfekt) et passé simple (Präteritum).',
 'Communication', 'courses/b1/02_reisen.mp4', 3200, 'courses/b1/02_reisen.pdf', 'PUBLISHED'),

(4, 3, 'Médias et technologie',
 'Internet et réseaux : die Website, das Smartphone, die App, soziale Netzwerke, herunterladen. Médias : die Zeitung, das Fernsehen, das Radio, der Podcast. Exprimer son opinion sur les médias : Ich finde, dass..., Meiner Meinung nach..., Es ist wichtig, dass... Subordonnées avec dass, weil, obwohl.',
 'Société', 'courses/b1/03_medien.mp4', 2800, 'courses/b1/03_medien.pdf', 'PUBLISHED'),

(5, 3, 'Environnement et écologie',
 'Problèmes environnementaux : der Klimawandel, die Umweltverschmutzung, das Recycling, erneuerbare Energie. Actions écologiques : Energie sparen, öffentliche Verkehrsmittel benutzen, Plastik vermeiden. Débat : Argumente für und gegen... Connecteurs logiques : deshalb, trotzdem, außerdem, jedoch. Passif au présent.',
 'Société', 'courses/b1/04_umwelt.mp4', 2900, 'courses/b1/04_umwelt.pdf', 'PUBLISHED'),

(6, 3, 'Éducation et système scolaire',
 'Système éducatif allemand : Grundschule, Gymnasium, Abitur, Universität, Berufsschule. Études en Allemagne : Studiengebühren, Stipendium, Bachelor, Master, Dissertation. Exprimer des souhaits et hypothèses : Ich würde gern..., Wenn ich... wäre/hätte... Konjunktiv II complet.',
 'Éducation', 'courses/b1/05_bildung.mp4', 3100, 'courses/b1/05_bildung.pdf', 'PUBLISHED'),

-- ===== NIVEAU B2 (level_id = 4) =====
(2, 4, 'Société et enjeux contemporains',
 'Thèmes de société : Migration, Integration, Gleichberechtigung, Generationenkonflikt, Digitalisierung. Exprimer nuances et réserves : Einerseits..., andererseits..., Es kommt darauf an, ob... Argumentation structurée : These, Argument, Beispiel, Gegenargument, Schlussfolgerung. Nominalisations et style soutenu.',
 'Société', 'courses/b2/01_gesellschaft.mp4', 3500, 'courses/b2/01_gesellschaft.pdf', 'PUBLISHED'),

(3, 4, 'Économie et monde des affaires',
 'Vocabulaire économique : die Wirtschaft, das Unternehmen, der Markt, die Investition, der Gewinn, der Verlust, die Inflation. Correspondance professionnelle avancée : formelle Emails, Berichte, Protokolle. Négociations : Konditionen, Vertragsverhandlungen. Chiffres et statistiques : Zunahme, Rückgang, stagnieren.',
 'Économie', 'courses/b2/02_wirtschaft.mp4', 3700, 'courses/b2/02_wirtschaft.pdf', 'PUBLISHED'),

(4, 4, 'Culture et arts germaniques',
 'Littérature allemande : Goethe, Schiller, Kafka, Brecht, Celan — œuvres et contexte. Cinéma germanophone : Herzog, Fassbinder, Das Leben der Anderen. Musique : Beethoven, Bach, Wagner, rammstein. Analyse culturelle comparée : Unterschiede zwischen Deutschland, Österreich und der Schweiz.',
 'Culture', 'courses/b2/03_kultur.mp4', 3300, 'courses/b2/03_kultur.pdf', 'PUBLISHED'),

(5, 4, 'Grammaire avancée et style',
 'Subordonnées relatives au génitif et datif. Infinitives avec zu, um... zu, ohne... zu, anstatt... zu. Participes présents et passés comme épithètes. Gérondif. Registres de langue : formell, informell, umgangssprachlich. Faux amis et pièges lexicaux. Rédaction de textes argumentatifs.',
 'Grammaire', 'courses/b2/04_grammatik.mp4', 4000, 'courses/b2/04_grammatik.pdf', 'PUBLISHED'),

-- ===== NIVEAU C1 (level_id = 5) =====
(2, 5, 'Langue académique et scientifique',
 'Rédaction académique : Einleitung, Hauptteil, Schluss, Fußnoten, Quellenangaben, Zitieren. Vocabulaire scientifique par domaine : Medizin, Recht, Technik, Wirtschaft, Philosophie. Comprendre des textes académiques denses. Présenter des recherches : Forschungsergebnisse vorstellen, Hypothesen aufstellen.',
 'Académique', 'courses/c1/01_akademisch.mp4', 4500, 'courses/c1/01_akademisch.pdf', 'PUBLISHED'),

(3, 5, 'Rhétorique et débat',
 'Art de la discussion : Diskussion leiten, moderieren, das Wort ergreifen, widersprechen, zustimmen. Figures rhétoriques : Metapher, Ironie, Hyperbel, Euphemismus. Discours public : Rede halten, überzeugen, Publikum ansprechen. Analyse de discours politiques allemands. Débat contradictoire.',
 'Communication avancée', 'courses/c1/02_rhetorik.mp4', 4200, 'courses/c1/02_rhetorik.pdf', 'PUBLISHED'),

(4, 5, 'Stylistique et nuances linguistiques',
 'Registres de langue et leur utilisation appropriée. Synonymes et antonymes avancés. Expressions idiomatiques et proverbes : Sprichwörter, Redewendungen. Style indirect libre. Subtilités grammaticales : Konjunktiv I pour le discours rapporté. Analyse stylistique de textes littéraires.',
 'Stylistique', 'courses/c1/03_stilistik.mp4', 4800, 'courses/c1/03_stilistik.pdf', 'PUBLISHED'),

(5, 5, 'Interculturalité et pragmatique',
 'Communication interculturelle : Missverständnisse vermeiden, kulturelle Codes verstehen. Pragmatique linguistique : actes de langage, implicatures, politesse négative. Humour et ironie en allemand. Dialectes régionaux : Bayerisch, Schwäbisch, Kölsch, Österreichisch, Schweizerdeutsch.',
 'Culture', 'courses/c1/04_interkultur.mp4', 4000, 'courses/c1/04_interkultur.pdf', 'PUBLISHED'),

-- ===== NIVEAU C2 (level_id = 6) =====
(2, 6, 'Maîtrise linguistique et certifications',
 'Préparation au Goethe-Zertifikat C2 et au TestDaF. Stratégies d''examen : Zeitmanagement, Leseverstehen, Hörverstehen, Schreiben, Sprechen. Textes authentiques : journaux, essais, littérature. Correction des dernières erreurs résiduelles. Production écrite de niveau natif.',
 'Certification', 'courses/c2/01_zertifizierung.mp4', 5400, 'courses/c2/01_zertifizierung.pdf', 'PUBLISHED'),

(3, 6, 'Analyse littéraire et textes classiques',
 'Lecture et analyse d''œuvres originales : Faust (Goethe), Der Prozess (Kafka), Die Verwandlung (Kafka), Steppenwolf (Hesse), Der Vorleser (Schlink). Analyse littéraire : Erzählperspektive, Symbolik, Figurenanalyse, historischer Kontext. Intertextualité. Rédaction d''essais critiques.',
 'Littérature', 'courses/c2/02_literatur.mp4', 5200, 'courses/c2/02_literatur.pdf', 'PUBLISHED'),

(4, 6, 'Expression orale native et spontanée',
 'Parler comme un natif : Kollokationen, chunk learning, formulaic language. Conversations authentiques sur tous sujets. Improvisation et expression spontanée. Réduire l''accent. Speed reading en allemand. Compréhension de l''allemand rapide, du langage familier et des sous-entendus culturels.',
 'Expression orale', 'courses/c2/03_muttersprache.mp4', 4800, 'courses/c2/03_muttersprache.pdf', 'PUBLISHED'),

(5, 6, 'Philosophie et pensée critique en allemand',
 'Grands philosophes allemands : Kant (Kritik der reinen Vernunft), Hegel (Dialektik), Nietzsche (Übermensch, Wille zur Macht), Heidegger (Sein und Zeit), Habermas (kommunikatives Handeln). Vocabulaire philosophique. Rédiger un essai philosophique argumenté en allemand de niveau universitaire.',
 'Philosophie', 'courses/c2/04_philosophie.mp4', 5600, 'courses/c2/04_philosophie.pdf', 'PUBLISHED');

-- ============================================================
-- QCMs — 3 QCM par niveau (18 au total)
-- ============================================================

INSERT INTO qcms (level_id, title, theme, created_by) VALUES
-- A1
(1, 'QCM A1 — Alphabet et prononciation', 'Phonétique', 2),
(1, 'QCM A1 — Salutations et présentations', 'Communication', 2),
(1, 'QCM A1 — Grammaire de base', 'Grammaire', 2),
-- A2
(2, 'QCM A2 — Vie quotidienne', 'Vocabulaire', 3),
(2, 'QCM A2 — Transports et directions', 'Communication', 3),
(2, 'QCM A2 — Grammaire A2', 'Grammaire', 3),
-- B1
(3, 'QCM B1 — Travail et société', 'Société', 4),
(3, 'QCM B1 — Expression et opinion', 'Communication', 4),
(3, 'QCM B1 — Grammaire B1', 'Grammaire', 4),
-- B2
(4, 'QCM B2 — Société et culture', 'Société', 5),
(4, 'QCM B2 — Économie et médias', 'Économie', 5),
(4, 'QCM B2 — Grammaire avancée', 'Grammaire', 5),
-- C1
(5, 'QCM C1 — Langue académique', 'Académique', 6),
(5, 'QCM C1 — Stylistique et nuances', 'Stylistique', 6),
(5, 'QCM C1 — Rhétorique et culture', 'Culture', 6),
-- C2
(6, 'QCM C2 — Maîtrise complète', 'Général', 2),
(6, 'QCM C2 — Littérature et analyse', 'Littérature', 2),
(6, 'QCM C2 — Philosophie et expression', 'Philosophie', 2);

-- ============================================================
-- QCM QUESTIONS & CHOICES
-- ============================================================

-- ===== QCM 1 : A1 — Alphabet et prononciation =====
INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
(1, 'Comment prononce-t-on la lettre "ä" en allemand ?', 'SINGLE_CHOICE', 1),
(1, 'Quel mot contient le son "sch" (comme "ch" dans "chat") ?', 'SINGLE_CHOICE', 2),
(1, 'Comment s''écrit "Je m''appelle" en allemand ?', 'SINGLE_CHOICE', 3),
(1, 'Quelle est la bonne prononciation de "ei" en allemand ?', 'SINGLE_CHOICE', 4),
(1, 'Quel symbole représente le son "ss" en allemand ?', 'SINGLE_CHOICE', 5),
(1, 'Comment dit-on l''alphabet allemand "Z" ?', 'SINGLE_CHOICE', 6),
(1, 'Le "v" allemand se prononce comme...', 'SINGLE_CHOICE', 7),
(1, 'Quel mot contient un umlaut ?', 'SINGLE_CHOICE', 8);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
-- Q1
(1, 'Comme le "e" fermé français dans "été"', TRUE, 'Le ä est un e ouvert, proche du è français dans "fête"'),
(1, 'Comme le "a" français standard', FALSE, 'Le a allemand et le ä sont différents — le ä est plus fermé'),
(1, 'Comme le "i" français', FALSE, 'Non, le ä n''a aucun rapport avec le i'),
(1, 'Comme le "ou" français', FALSE, 'Non, le ä est une voyelle antérieure ouverte'),
-- Q2
(5, 'Schule', TRUE, 'Schule (école) — le "sch" se prononce comme "ch" dans "chat"'),
(5, 'Sport', FALSE, 'Sport contient "sp" qui se prononce "chp" en début de mot, pas "sch"'),
(5, 'Stern', FALSE, 'Stern (étoile) contient "st" initial qui se prononce "cht"'),
(5, 'Sand', FALSE, 'Sand ne contient pas de "sch"'),
-- Q3
(9, 'Ich heiße', TRUE, '"Ich heiße" est la traduction exacte de "Je m''appelle"'),
(9, 'Ich bin', FALSE, '"Ich bin" signifie "Je suis", pas "Je m''appelle"'),
(9, 'Mein Name', FALSE, '"Mein Name" signifie "Mon nom" — incomplet sans verbe'),
(9, 'Ich komme', FALSE, '"Ich komme" signifie "Je viens"'),
-- Q4
(13, 'Comme "aï" en français (ex: aïe)', TRUE, '"ei" se prononce [aɪ] comme dans "Wein" (vin)'),
(13, 'Comme "é" en français', FALSE, '"ei" ne se prononce pas "é" en allemand'),
(13, 'Comme "ee" long', FALSE, '"ee" long est représenté par "ie" en allemand'),
(13, 'Comme "wa" en français', FALSE, 'Non, "ei" est une diphtongue [aɪ]'),
-- Q5
(17, 'ß (Eszett)', TRUE, 'Le ß représente le son "ss" long, ex: Straße (rue), heiß (chaud)'),
(17, 'sz', FALSE, 'sz existe mais ß est le symbole officiel pour "ss" long'),
(17, 'ss', FALSE, 'ss et ß existent tous les deux mais dans des contextes différents'),
(17, 'ts', FALSE, 'ts représente un autre son en allemand'),
-- Q6
(21, '"Tset"', TRUE, 'En allemand, Z se prononce "tset" [tsɛt]'),
(21, '"Zed"', FALSE, '"Zed" est la prononciation anglaise'),
(21, '"Zi"', FALSE, '"Zi" n''existe pas en allemand'),
(21, '"Ze"', FALSE, '"Ze" est la prononciation française mais pas allemande'),
-- Q7
(25, 'Comme le "f" français', TRUE, 'Le "v" allemand se prononce "f" : Vater [fa:tɐ], vier [fi:ɐ]'),
(25, 'Comme le "v" français dans "voiture"', FALSE, 'Non, en allemand natif "v" = "f", sauf dans les mots étrangers'),
(25, 'Comme le "w" français', FALSE, 'Le "w" allemand se prononce "v" (pas le "v" allemand)'),
(25, 'Muet', FALSE, 'Le "v" n''est jamais muet en allemand'),
-- Q8
(29, 'Mädchen', TRUE, 'Mädchen (fille) contient le umlaut "ä"'),
(29, 'Mutter', FALSE, 'Mutter (mère) ne contient aucun umlaut'),
(29, 'Kind', FALSE, 'Kind (enfant) ne contient aucun umlaut'),
(29, 'Mann', FALSE, 'Mann (homme) ne contient aucun umlaut');

-- ===== QCM 2 : A1 — Salutations et présentations =====
INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
(2, 'Comment dit-on "Bonsoir" en allemand ?', 'SINGLE_CHOICE', 1),
(2, 'Quelle est la traduction de "Woher kommen Sie?" ?', 'SINGLE_CHOICE', 2),
(2, 'Comment demander "Quel âge avez-vous ?" en allemand ?', 'SINGLE_CHOICE', 3),
(2, '"Ich wohne in Kamerun" signifie...', 'SINGLE_CHOICE', 4),
(2, 'Comment dit-on "Au revoir" de façon formelle ?', 'SINGLE_CHOICE', 5),
(2, 'Quelle phrase signifie "Je suis étudiant(e)" ?', 'SINGLE_CHOICE', 6),
(2, 'Comment répondre à "Wie geht es Ihnen?" poliment ?', 'SINGLE_CHOICE', 7),
(2, '"Ich spreche ein bisschen Deutsch" signifie...', 'SINGLE_CHOICE', 8);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
-- Q1 (id 33)
(33, 'Guten Abend', TRUE, 'Guten Abend = Bonsoir (après 18h environ)'),
(33, 'Guten Morgen', FALSE, 'Guten Morgen = Bonjour (le matin)'),
(33, 'Guten Tag', FALSE, 'Guten Tag = Bonjour (la journée)'),
(33, 'Gute Nacht', FALSE, 'Gute Nacht = Bonne nuit (avant de dormir)'),
-- Q2 (id 34)
(34, 'D''où venez-vous ?', TRUE, '"Woher kommen Sie?" = D''où venez-vous ? (forme polie)'),
(34, 'Comment vous appelez-vous ?', FALSE, 'Wie heißen Sie? = Comment vous appelez-vous?'),
(34, 'Où habitez-vous ?', FALSE, 'Wo wohnen Sie? = Où habitez-vous?'),
(34, 'Comment allez-vous ?', FALSE, 'Wie geht es Ihnen? = Comment allez-vous?'),
-- Q3 (id 35)
(35, 'Wie alt sind Sie?', TRUE, 'Wie alt sind Sie? = Quel âge avez-vous ? (formel)'),
(35, 'Wie heißen Sie?', FALSE, 'Wie heißen Sie? = Comment vous appelez-vous?'),
(35, 'Wo kommen Sie her?', FALSE, 'Wo kommen Sie her? = D''où venez-vous? (variante)'),
(35, 'Was machen Sie?', FALSE, 'Was machen Sie? = Que faites-vous (dans la vie)?'),
-- Q4 (id 36)
(36, 'J''habite au Cameroun', TRUE, '"Ich wohne in Kamerun" = J''habite au Cameroun'),
(36, 'Je viens du Cameroun', FALSE, '"Je viens du Cameroun" = Ich komme aus Kamerun'),
(36, 'Je connais le Cameroun', FALSE, 'Cela se dirait : Ich kenne Kamerun'),
(36, 'J''aime le Cameroun', FALSE, 'Cela se dirait : Ich mag Kamerun / Ich liebe Kamerun'),
-- Q5 (id 37)
(37, 'Auf Wiedersehen', TRUE, 'Auf Wiedersehen = Au revoir (formule formelle et standard)'),
(37, 'Tschüss', FALSE, 'Tschüss est informel, utilisé entre amis'),
(37, 'Ciao', FALSE, 'Ciao est très familier, d''origine italienne'),
(37, 'Bis später', FALSE, 'Bis später = À tout à l''heure'),
-- Q6 (id 38)
(38, 'Ich bin Student/Studentin', TRUE, 'Ich bin Student (m) / Studentin (f) = Je suis étudiant(e)'),
(38, 'Ich lerne Student', FALSE, 'Lerne n''est pas utilisé pour l''identité professionnelle'),
(38, 'Ich arbeite als Student', FALSE, 'On ne "travaille" pas comme étudiant, on "est" étudiant'),
(38, 'Ich habe Student', FALSE, 'Haben ne s''utilise pas pour les professions'),
-- Q7 (id 39)
(39, 'Danke, gut. Und Ihnen?', TRUE, 'Réponse polie : Merci, bien. Et vous ? — très usuel'),
(39, 'Ja, ich bin gut', FALSE, '"Ich bin gut" est un anglicisme — en allemand on dit "Mir geht es gut"'),
(39, 'Gut Morgen', FALSE, 'Gut Morgen n''existe pas — c''est Guten Morgen'),
(39, 'Ich weiß nicht', FALSE, '"Je ne sais pas" n''est pas une réponse appropriée ici'),
-- Q8 (id 40)
(40, 'Je parle un peu allemand', TRUE, '"Ich spreche ein bisschen Deutsch" = Je parle un peu allemand'),
(40, 'J''apprends l''allemand', FALSE, '"J''apprends l''allemand" = Ich lerne Deutsch'),
(40, 'Je comprends l''allemand', FALSE, '"Je comprends l''allemand" = Ich verstehe Deutsch'),
(40, 'J''aime l''allemand', FALSE, '"J''aime l''allemand" = Ich mag Deutsch / Ich lerne gern Deutsch');

-- ===== QCM 3 : A1 — Grammaire de base =====
INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
(3, 'Quel est l''article défini masculin en allemand ?', 'SINGLE_CHOICE', 1),
(3, 'Conjuguez "sein" à la 3ème personne du singulier.', 'SINGLE_CHOICE', 2),
(3, 'Quel est le pluriel de "das Kind" (l''enfant) ?', 'SINGLE_CHOICE', 3),
(3, '"Ich habe ___ Bruder." Quel article indéfini utiliser ?', 'SINGLE_CHOICE', 4),
(3, 'Quel pronom sujet correspond à "vous" (formel) ?', 'SINGLE_CHOICE', 5),
(3, 'Comment conjugue-t-on "lernen" à "ich" ?', 'SINGLE_CHOICE', 6),
(3, 'Quel est le genre de "die Mutter" ?', 'SINGLE_CHOICE', 7),
(3, '"___ ist meine Schwester." Quel pronom utiliser ?', 'SINGLE_CHOICE', 8);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(41, 'der', TRUE, 'der = article masculin, die = féminin, das = neutre'),
(41, 'die', FALSE, 'die est l''article féminin et aussi l''article pluriel'),
(41, 'das', FALSE, 'das est l''article neutre'),
(41, 'den', FALSE, 'den est l''article masculin à l''accusatif'),
(42, 'ist', TRUE, 'er/sie/es ist = il/elle est (conjugaison de sein)'),
(42, 'sind', FALSE, 'sind = sommes/êtes/sont (wir/ihr/sie/Sie)'),
(42, 'bin', FALSE, 'bin = suis (ich bin)'),
(42, 'bist', FALSE, 'bist = es (du bist)'),
(43, 'die Kinder', TRUE, 'Kind → Kinder (pluriel avec -er et umlaut impossible car i)'),
(43, 'das Kinds', FALSE, 'L''allemand ne forme pas les pluriels avec -s en général'),
(43, 'die Kind', FALSE, 'die Kind n''existe pas — pluriel sans marque'),
(43, 'der Kinder', FALSE, 'L''article du pluriel est toujours "die"'),
(44, 'einen', TRUE, 'einen est l''article indéfini masculin à l''accusatif (Bruder = masculin, COD)'),
(44, 'ein', FALSE, 'ein est le nominatif masculin/neutre — ici c''est l''accusatif'),
(44, 'eine', FALSE, 'eine est le féminin'),
(44, 'eines', FALSE, 'eines est le génitif neutre'),
(45, 'Sie', TRUE, 'Sie (majuscule) = vous formel en allemand — toujours avec majuscule'),
(45, 'ihr', FALSE, 'ihr = vous informel (pluriel, entre amis)'),
(45, 'du', FALSE, 'du = tu (singulier informel)'),
(45, 'wir', FALSE, 'wir = nous'),
(46, 'ich lerne', TRUE, 'lernen → ich lerne (radical + -e pour ich)'),
(46, 'ich lernt', FALSE, 'lernt = er/sie/es lernt (3ème personne)'),
(46, 'ich lernen', FALSE, 'lernen est l''infinitif, pas la forme conjuguée'),
(46, 'ich lernst', FALSE, 'lernst = du lernst (2ème personne)'),
(47, 'Féminin', TRUE, 'die Mutter — "die" indique le genre féminin'),
(47, 'Masculin', FALSE, 'Masculin aurait "der" : der Vater'),
(47, 'Neutre', FALSE, 'Neutre aurait "das" : das Kind'),
(47, 'On ne peut pas savoir', FALSE, 'L''article "die" indique clairement le féminin'),
(48, 'Sie', TRUE, 'Sie = elle (pronom féminin 3ème personne) — Schwester est féminin'),
(48, 'Er', FALSE, 'Er = il (masculin) — Schwester est féminin'),
(48, 'Es', FALSE, 'Es = il/elle neutre — Schwester est féminin'),
(48, 'Wir', FALSE, 'Wir = nous');

-- ===== QCM 4 : A2 — Vie quotidienne =====
INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
(4, '"Was kostet das?" signifie...', 'SINGLE_CHOICE', 1),
(4, 'Comment dit-on "la boulangerie" en allemand ?', 'SINGLE_CHOICE', 2),
(4, '"Ich bin krank" signifie...', 'SINGLE_CHOICE', 3),
(4, 'Quel repas est "das Frühstück" ?', 'SINGLE_CHOICE', 4),
(4, '"Ich möchte ein Zimmer reservieren" signifie...', 'SINGLE_CHOICE', 5),
(4, 'Comment dit-on "le supermarché" ?', 'SINGLE_CHOICE', 6),
(4, '"Rechts" signifie...', 'SINGLE_CHOICE', 7),
(4, 'Quel vêtement est "die Jacke" ?', 'SINGLE_CHOICE', 8);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(49, 'Combien ça coûte ?', TRUE, '"Was kostet das?" = Combien ça coûte ? / Quel est le prix ?'),
(49, 'Qu''est-ce que c''est ?', FALSE, '"Qu''est-ce que c''est ?" = Was ist das?'),
(49, 'Est-ce cher ?', FALSE, '"Est-ce cher ?" = Ist das teuer?'),
(49, 'Tu veux quoi ?', FALSE, '"Was möchtest du?" = Que veux-tu ?'),
(50, 'die Bäckerei', TRUE, 'die Bäckerei = la boulangerie (où on vend du pain)'),
(50, 'die Metzgerei', FALSE, 'die Metzgerei = la boucherie'),
(50, 'die Apotheke', FALSE, 'die Apotheke = la pharmacie'),
(50, 'die Drogerie', FALSE, 'die Drogerie = la droguerie / parapharmacie'),
(51, 'Je suis malade', TRUE, '"Ich bin krank" = Je suis malade'),
(51, 'J''ai faim', FALSE, '"J''ai faim" = Ich habe Hunger'),
(51, 'Je suis fatigué(e)', FALSE, '"Je suis fatigué(e)" = Ich bin müde'),
(51, 'J''ai mal', FALSE, '"J''ai mal" = Ich habe Schmerzen'),
(52, 'Le petit-déjeuner', TRUE, 'das Frühstück = le petit-déjeuner (repas du matin)'),
(52, 'Le déjeuner', FALSE, 'Le déjeuner = das Mittagessen'),
(52, 'Le dîner', FALSE, 'Le dîner = das Abendessen'),
(52, 'Le goûter', FALSE, 'Le goûter = die Zwischenmahlzeit / der Imbiss'),
(53, 'Je voudrais réserver une chambre', TRUE, '"Ich möchte ein Zimmer reservieren" = Je voudrais réserver une chambre'),
(53, 'J''ai réservé une chambre', FALSE, '"J''ai réservé" = Ich habe ein Zimmer reserviert (Perfekt)'),
(53, 'Je cherche une chambre', FALSE, '"Je cherche" = Ich suche ein Zimmer'),
(53, 'La chambre est réservée', FALSE, '"Das Zimmer ist reserviert" = La chambre est réservée'),
(54, 'der Supermarkt', TRUE, 'der Supermarkt = le supermarché (masculin)'),
(54, 'das Supermarkt', FALSE, 'Supermarkt est masculin : der Supermarkt'),
(54, 'die Supermarkt', FALSE, 'Supermarkt est masculin : der Supermarkt'),
(54, 'der Markt', FALSE, 'der Markt = le marché (pas le supermarché)'),
(55, 'À droite', TRUE, '"Rechts" = à droite — son contraire est "links" (à gauche)'),
(55, 'À gauche', FALSE, '"À gauche" = links'),
(55, 'Tout droit', FALSE, '"Tout droit" = geradeaus'),
(55, 'En face', FALSE, '"En face" = gegenüber'),
(56, 'La veste / le manteau', TRUE, 'die Jacke = la veste légère ou le blouson'),
(56, 'La robe', FALSE, 'La robe = das Kleid'),
(56, 'Le pantalon', FALSE, 'Le pantalon = die Hose'),
(56, 'La chemise', FALSE, 'La chemise = das Hemd (homme) / die Bluse (femme)');

-- ===== QCM 5 : A2 — Transports et directions =====
INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
(5, 'Comment dit-on "le métro" en allemand ?', 'SINGLE_CHOICE', 1),
(5, '"Wo ist der Bahnhof?" signifie...', 'SINGLE_CHOICE', 2),
(5, '"Eine Hin- und Rückfahrkarte" est...', 'SINGLE_CHOICE', 3),
(5, 'La préposition "in" utilise quel cas pour l''emplacement statique ?', 'SINGLE_CHOICE', 4),
(5, 'Comment dit-on "Tournez à gauche" en allemand ?', 'SINGLE_CHOICE', 5),
(5, '"Das Flugzeug" est...', 'SINGLE_CHOICE', 6),
(5, 'Comment demander "Excusez-moi, où est...?" formellement ?', 'SINGLE_CHOICE', 7),
(5, '"Gegenüber dem Bahnhof" signifie...', 'SINGLE_CHOICE', 8);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(57, 'die U-Bahn', TRUE, 'die U-Bahn = le métro (Untergrundbahn = train souterrain)'),
(57, 'die S-Bahn', FALSE, 'die S-Bahn = le RER / train de banlieue (Stadtschnellbahn)'),
(57, 'der Bus', FALSE, 'der Bus = le bus'),
(57, 'die Straßenbahn', FALSE, 'die Straßenbahn = le tramway'),
(58, 'Où est la gare ?', TRUE, '"Wo ist der Bahnhof?" = Où est la gare ?'),
(58, 'Comment aller à la gare ?', FALSE, '"Wie komme ich zum Bahnhof?" = Comment aller à la gare ?'),
(58, 'À quelle heure part le train ?', FALSE, '"Wann fährt der Zug ab?" = À quelle heure part le train ?'),
(58, 'La gare est loin ?', FALSE, '"Ist der Bahnhof weit?" = La gare est-elle loin ?'),
(59, 'Un aller-retour', TRUE, '"Hin- und Rückfahrkarte" = billet aller-retour (Hin = aller, Rück = retour)'),
(59, 'Un aller simple', FALSE, '"Einfache Fahrkarte" = aller simple'),
(59, 'Un abonnement mensuel', FALSE, '"Monatskarte" = abonnement mensuel'),
(59, 'Un billet de groupe', FALSE, '"Gruppenticket" = billet de groupe'),
(60, 'Le datif', TRUE, 'Emplacement statique (wo?) → datif : "Ich bin in der Stadt"'),
(60, 'L''accusatif', FALSE, 'L''accusatif indique le mouvement vers (wohin?) : "Ich gehe in die Stadt"'),
(60, 'Le nominatif', FALSE, 'Le nominatif est pour le sujet, pas les prépositions de lieu'),
(60, 'Le génitif', FALSE, 'Le génitif exprime la possession, pas le lieu avec "in"'),
(61, 'Biegen Sie links ab', TRUE, '"Biegen Sie links ab" = Tournez à gauche (forme polie)'),
(61, 'Gehen Sie links', FALSE, '"Gehen Sie links" est possible mais moins précis pour "tourner"'),
(61, 'Links fahren', FALSE, 'Links fahren = conduire à gauche — pas une instruction de direction'),
(61, 'Nach links drehen', FALSE, '"Nach links drehen" s''utilise plutôt pour un volant, pas pour donner une direction'),
(62, 'L''avion', TRUE, 'das Flugzeug = l''avion (Flug = vol, Zeug = engin)'),
(62, 'Le bateau', FALSE, 'Le bateau = das Schiff / das Boot'),
(62, 'Le vélo', FALSE, 'Le vélo = das Fahrrad'),
(62, 'La voiture', FALSE, 'La voiture = das Auto'),
(63, 'Entschuldigung, wo ist...?', TRUE, '"Entschuldigung, wo ist...?" = Excusez-moi, où est... ? (formule standard)'),
(63, 'Bitte, Bahnhof?', FALSE, 'Trop brusque et incomplet comme formulation'),
(63, 'Ich suche...', FALSE, '"Ich suche..." = Je cherche... — moins direct pour demander où se trouve qqch'),
(63, 'Können Sie mir helfen, wo...?', FALSE, 'Correct mais plus complexe — "Entschuldigung, wo ist...?" est plus naturel'),
(64, 'En face de la gare', TRUE, '"Gegenüber" = en face de — "gegenüber dem Bahnhof" = en face de la gare'),
(64, 'À côté de la gare', FALSE, '"À côté de" = neben dem Bahnhof'),
(64, 'Derrière la gare', FALSE, '"Derrière" = hinter dem Bahnhof'),
(64, 'Avant la gare', FALSE, '"Avant" = vor dem Bahnhof');

-- ===== QCM 6 : A2 — Grammaire A2 =====
INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
(6, 'Quel est le Perfekt de "gehen" (aller) ?', 'SINGLE_CHOICE', 1),
(6, 'Comment forme-t-on le Perfekt avec "haben" ?', 'SINGLE_CHOICE', 2),
(6, 'Quelle est la forme correcte : "Ich ___ gestern ins Kino gegangen."', 'SINGLE_CHOICE', 3),
(6, '"Wegen des Regens" utilise quel cas ?', 'SINGLE_CHOICE', 4),
(6, 'Comment dit-on "Je devais partir" (obligation passée) ?', 'SINGLE_CHOICE', 5),
(6, 'Quel est l''impératif de "kommen" pour "Sie" ?', 'SINGLE_CHOICE', 6),
(6, 'Quelle préposition utilise toujours l''accusatif ?', 'SINGLE_CHOICE', 7),
(6, '"Obwohl es regnet, gehe ich spazieren." "Obwohl" introduit...', 'SINGLE_CHOICE', 8);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(65, 'ist gegangen', TRUE, 'Gehen forme son Perfekt avec "sein" + participe "gegangen"'),
(65, 'hat gegangen', FALSE, 'Gehen utilise "sein" (mouvement) et non "haben"'),
(65, 'hat gegeht', FALSE, '"gegeht" n''existe pas — le participe de gehen est "gegangen"'),
(65, 'war gegangen', FALSE, '"war gegangen" est le Plusquamperfekt (plus-que-parfait)'),
(66, 'haben + participe passé', TRUE, 'La plupart des verbes transitifs forment le Perfekt avec haben + Partizip II'),
(66, 'sein + infinitif', FALSE, 'sein + infinitif est une périphrase modale, pas le Perfekt'),
(66, 'werden + participe', FALSE, 'werden + participe est le passif (Passiv)'),
(66, 'haben + infinitif', FALSE, 'haben + infinitif ne forme pas le Perfekt'),
(67, 'bin', TRUE, '"Ich bin gestern ins Kino gegangen" — gehen se conjugue avec sein'),
(67, 'habe', FALSE, 'Gehen utilise sein et non haben au Perfekt'),
(67, 'war', FALSE, '"war gegangen" serait le Plusquamperfekt'),
(67, 'hatte', FALSE, '"hatte gegangen" n''est pas correct'),
(68, 'Le génitif', TRUE, '"Wegen" régit le génitif : wegen des Regens (à cause de la pluie)'),
(68, 'Le datif', FALSE, 'Dans la langue parlée "wegen" + datif s''entend, mais le génitif est correct'),
(68, 'L''accusatif', FALSE, '"Wegen" ne prend pas l''accusatif'),
(68, 'Le nominatif', FALSE, '"Wegen" ne prend jamais le nominatif'),
(69, 'Ich musste gehen', TRUE, '"Ich musste gehen" = Je devais partir (Präteritum de müssen)'),
(69, 'Ich muss gegangen', FALSE, 'Forme incorrecte — le modal reste à l''infinitif avec Perfekt'),
(69, 'Ich hatte gehen müssen', FALSE, 'C''est le Plusquamperfekt modal — correct mais plus complexe'),
(69, 'Ich sollte gehen', FALSE, '"Ich sollte gehen" = Je devais partir (obligation externe) — müssen est plus fort'),
(70, 'Kommen Sie!', TRUE, '"Kommen Sie!" = Venez ! (impératif formel Sie)'),
(70, 'Komm!', FALSE, '"Komm!" est l''impératif informel singulier (du)'),
(70, 'Kommt!', FALSE, '"Kommt!" est l''impératif pluriel informel (ihr)'),
(70, 'Kommen!', FALSE, '"Kommen!" seul n''est pas une forme d''impératif standard'),
(71, 'durch', TRUE, '"Durch" (à travers) régit toujours l''accusatif'),
(71, 'mit', FALSE, '"Mit" régit toujours le datif'),
(71, 'bei', FALSE, '"Bei" régit toujours le datif'),
(71, 'von', FALSE, '"Von" régit toujours le datif'),
(72, 'Une proposition concessive', TRUE, '"Obwohl" = bien que / même si — introduit une concession'),
(72, 'Une proposition causale', FALSE, 'La cause s''introduit avec "weil" ou "da"'),
(72, 'Une proposition temporelle', FALSE, 'Le temps s''introduit avec "wenn", "als", "während"'),
(72, 'Une proposition conditionnelle', FALSE, 'La condition s''introduit avec "wenn" ou "falls"');

-- ===== QCM 7 : B1 — Travail et société =====
INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
(7, 'Comment dit-on "le curriculum vitae" en allemand ?', 'SINGLE_CHOICE', 1),
(7, '"Ich würde gern mehr verdienen" signifie...', 'SINGLE_CHOICE', 2),
(7, 'Quel mot signifie "le chômage" ?', 'SINGLE_CHOICE', 3),
(7, '"Meiner Meinung nach" signifie...', 'SINGLE_CHOICE', 4),
(7, 'Comment exprimer la condition avec Konjunktiv II ?', 'SINGLE_CHOICE', 5),
(7, '"Das Stipendium" est...', 'SINGLE_CHOICE', 6),
(7, '"Die Ausbildung" fait référence à...', 'SINGLE_CHOICE', 7),
(7, 'Comment dit-on "Je travaille à plein temps" ?', 'SINGLE_CHOICE', 8);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(73, 'der Lebenslauf', TRUE, 'der Lebenslauf = le CV (Lebens = vie, Lauf = déroulement)'),
(73, 'das Bewerbungsschreiben', FALSE, 'das Bewerbungsschreiben = la lettre de motivation'),
(73, 'das Zeugnis', FALSE, 'das Zeugnis = le bulletin / l''attestation / le diplôme'),
(73, 'die Bewerbung', FALSE, 'die Bewerbung = la candidature (ensemble du dossier)'),
(74, 'J''aimerais gagner plus', TRUE, '"Ich würde gern mehr verdienen" = J''aimerais gagner plus (Konjunktiv II)'),
(74, 'Je gagne beaucoup', FALSE, '"Je gagne beaucoup" = Ich verdiene viel'),
(74, 'J''ai besoin d''un travail', FALSE, '"J''ai besoin d''un travail" = Ich brauche einen Job'),
(74, 'Je cherche un meilleur poste', FALSE, '"Je cherche un meilleur poste" = Ich suche eine bessere Stelle'),
(75, 'die Arbeitslosigkeit', TRUE, 'die Arbeitslosigkeit = le chômage'),
(75, 'die Arbeit', FALSE, 'die Arbeit = le travail'),
(75, 'der Arbeitsplatz', FALSE, 'der Arbeitsplatz = le poste de travail / l''emploi'),
(75, 'die Arbeitszeit', FALSE, 'die Arbeitszeit = le temps de travail'),
(76, 'À mon avis', TRUE, '"Meiner Meinung nach" = À mon avis / Selon moi'),
(76, 'En ce qui me concerne', FALSE, '"En ce qui me concerne" = Was mich betrifft'),
(76, 'Normalement', FALSE, '"Normalement" = Normalerweise'),
(76, 'Il me semble que', FALSE, '"Il me semble que" = Es scheint mir, dass'),
(77, 'Wenn ich Zeit hätte, würde ich reisen', TRUE, 'Structure Konjunktiv II : wenn + hätte/wäre + würde + infinitif'),
(77, 'Wenn ich Zeit habe, reise ich', FALSE, 'C''est une condition réelle (Indikativ) au présent'),
(77, 'Als ich Zeit hatte, reiste ich', FALSE, '"Als" introduit le passé — pas une condition hypothétique'),
(77, 'Obwohl ich Zeit habe, reise ich', FALSE, '"Obwohl" = bien que — pas une condition'),
(78, 'Une bourse d''études', TRUE, 'das Stipendium = la bourse (d''études ou de recherche)'),
(78, 'Les frais de scolarité', FALSE, 'Les frais de scolarité = die Studiengebühren'),
(78, 'Un prêt étudiant', FALSE, 'Un prêt étudiant = das Studiendarlehen'),
(78, 'Un diplôme', FALSE, 'Un diplôme = der Abschluss / das Diplom'),
(79, 'Une formation professionnelle', TRUE, 'die Ausbildung = la formation / l''apprentissage professionnel (dual system)'),
(79, 'L''université', FALSE, 'L''université = die Universität / die Hochschule'),
(79, 'L''éducation en général', FALSE, 'L''éducation en général = die Bildung'),
(79, 'Un cours de langues', FALSE, 'Un cours de langues = der Sprachkurs'),
(80, 'Ich arbeite Vollzeit', TRUE, '"Vollzeit" = à plein temps — son contraire est "Teilzeit" (à temps partiel)'),
(80, 'Ich arbeite viel', FALSE, '"Ich arbeite viel" = Je travaille beaucoup (quantité, pas statut)'),
(80, 'Ich bin angestellt', FALSE, '"Ich bin angestellt" = Je suis salarié (statut, pas horaire)'),
(80, 'Ich arbeite immer', FALSE, '"Ich arbeite immer" = Je travaille toujours');

-- ===== QCM 8 : B1 — Expression et opinion =====
INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
(8, 'Comment introduire un argument contraire en allemand ?', 'SINGLE_CHOICE', 1),
(8, '"Ich finde, dass die Umwelt wichtig ist." Quelle structure grammaticale ?', 'SINGLE_CHOICE', 2),
(8, 'Quel connecteur exprime la conséquence ?', 'SINGLE_CHOICE', 3),
(8, '"Weder... noch..." signifie...', 'SINGLE_CHOICE', 4),
(8, 'Comment exprimer la concession en B1 ?', 'SINGLE_CHOICE', 5),
(8, '"Je dois absolument partir" se dit...', 'SINGLE_CHOICE', 6),
(8, '"Es kommt darauf an" signifie...', 'SINGLE_CHOICE', 7),
(8, 'Quel registre est "Ich hau ab" ?', 'SINGLE_CHOICE', 8);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(81, 'Allerdings / Jedoch', TRUE, '"Allerdings" et "Jedoch" introduisent un argument contraire (cependant, toutefois)'),
(81, 'Deshalb', FALSE, '"Deshalb" exprime la conséquence (c''est pourquoi)'),
(81, 'Außerdem', FALSE, '"Außerdem" ajoute un argument (de plus, en outre)'),
(81, 'Zunächst', FALSE, '"Zunächst" signifie "d''abord"'),
(82, 'Proposition subordonnée avec "dass" (verbe en fin)', TRUE, '"dass" introduit une subordonnée — le verbe va en fin de proposition'),
(82, 'Proposition principale normale', FALSE, 'Après "dass", le verbe est en position finale, pas principale'),
(82, 'Infinitive', FALSE, 'Une infinitive n''a pas de sujet explicite'),
(82, 'Proposition relative', FALSE, 'Une relative est introduite par un pronom relatif, pas "dass"'),
(83, 'deshalb / daher / deswegen', TRUE, '"Deshalb/daher/deswegen" = donc / c''est pourquoi (conséquence)'),
(83, 'obwohl', FALSE, '"Obwohl" = bien que (concession)'),
(83, 'weil', FALSE, '"Weil" = parce que (cause)'),
(83, 'falls', FALSE, '"Falls" = au cas où (condition)'),
(84, 'Ni... ni...', TRUE, '"Weder... noch..." = ni... ni... — double négation'),
(84, 'Ou... ou...', FALSE, '"Ou... ou..." = entweder... oder...'),
(84, 'Et... et...', FALSE, '"Et... et..." = sowohl... als auch...'),
(84, 'Soit... soit...', FALSE, '"Soit... soit..." = entweder... oder... aussi'),
(85, 'Trotzdem / Zwar... aber', TRUE, '"Trotzdem" (quand même) et "Zwar...aber" (certes...mais) expriment la concession'),
(85, 'Weil / Da', FALSE, 'Weil et Da expriment la cause'),
(85, 'Also / Somit', FALSE, 'Also et Somit expriment la conséquence'),
(85, 'Wenn / Falls', FALSE, 'Wenn et Falls expriment la condition'),
(86, 'Ich muss unbedingt gehen', TRUE, '"Unbedingt" = absolument — renforce le modal "müssen"'),
(86, 'Ich gehe jetzt', FALSE, '"Ich gehe jetzt" = Je pars maintenant — pas l''obligation'),
(86, 'Ich sollte gehen', FALSE, '"Ich sollte gehen" = Je devrais partir (moins fort)'),
(86, 'Ich will gehen', FALSE, '"Ich will gehen" = Je veux partir (volonté, pas obligation)'),
(87, 'Ça dépend', TRUE, '"Es kommt darauf an" = Ça dépend — expression très usuelle'),
(87, 'C''est important', FALSE, '"C''est important" = Es ist wichtig'),
(87, 'Il faut voir', FALSE, '"Il faut voir" = Man muss sehen / Das muss man sehen'),
(87, 'C''est possible', FALSE, '"C''est possible" = Es ist möglich / Das ist möglich'),
(88, 'Familier / argotique', TRUE, '"Ich hau ab" = Je me tire — très familier, non standard'),
(88, 'Formel', FALSE, 'Le registre formel serait "Ich verabschiede mich"'),
(88, 'Standard neutre', FALSE, 'Le neutre serait "Ich gehe jetzt"'),
(88, 'Littéraire', FALSE, 'Le littéraire serait "Ich nehme Abschied"');

-- ===== QCM 9 : B1 — Grammaire B1 =====
INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
(9, 'Quel est le Konjunktiv II de "haben" à la 1ère personne ?', 'SINGLE_CHOICE', 1),
(9, 'Comment forme-t-on le passif présent ?', 'SINGLE_CHOICE', 2),
(9, '"Das Buch, ___ ich lese, ist interessant." Quel pronom relatif ?', 'SINGLE_CHOICE', 3),
(9, 'Quel est le Genitiv de "der Mann" ?', 'SINGLE_CHOICE', 4),
(9, '"Um die Grammatik zu verstehen, muss man viel üben." Cette structure exprime...', 'SINGLE_CHOICE', 5),
(9, 'Quelle est la différence entre "als" et "wenn" ?', 'SINGLE_CHOICE', 6),
(9, '"Je voudrais que tu viennes" en allemand...', 'SINGLE_CHOICE', 7),
(9, 'Quel adjectif est au bon cas : "Er hilft ___ alten Mann."', 'SINGLE_CHOICE', 8);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(89, 'ich hätte', TRUE, 'Konjunktiv II de haben : hätte (ich hätte, du hättest, er hätte...)'),
(89, 'ich habe', FALSE, '"ich habe" est l''indicatif présent'),
(89, 'ich hatte', FALSE, '"ich hatte" est le Präteritum indicatif'),
(89, 'ich wäre', FALSE, '"ich wäre" est le Konjunktiv II de "sein"'),
(90, 'werden + Partizip II', TRUE, 'Passif présent : Das Buch wird gelesen (Le livre est lu)'),
(90, 'sein + Partizip II', FALSE, '"Sein + Partizip II" est le passif d''état (Zustandspassiv)'),
(90, 'haben + Partizip II', FALSE, '"Haben + Partizip II" est le Perfekt actif'),
(90, 'werden + Infinitif', FALSE, '"Werden + Infinitif" est le futur'),
(91, 'das', TRUE, '"Das Buch" est neutre accusatif → pronom relatif "das"'),
(91, 'der', FALSE, '"der" est masculin nominatif ou féminin/pluriel génitif'),
(91, 'die', FALSE, '"die" est pour le féminin ou le pluriel'),
(91, 'dem', FALSE, '"dem" est le datif masculin/neutre'),
(92, 'des Mannes', TRUE, 'Génitif masculin : der Mann → des Mannes (avec -es ou -s)'),
(92, 'dem Mann', FALSE, '"dem Mann" est le datif'),
(92, 'den Mann', FALSE, '"den Mann" est l''accusatif'),
(92, 'der Mannes', FALSE, 'Le génitif masculin est "des" pas "der"'),
(93, 'Le but / la finalité', TRUE, '"Um... zu..." exprime le but : pour (faire quelque chose)'),
(93, 'La cause', FALSE, 'La cause s''exprime avec "weil" ou "da"'),
(93, 'La concession', FALSE, 'La concession s''exprime avec "obwohl" ou "trotzdem"'),
(93, 'La conséquence', FALSE, 'La conséquence s''exprime avec "deshalb" ou "sodass"'),
(94, '"Als" pour événement passé unique, "wenn" pour habitude/présent/futur', TRUE, '"Als" = quand (une seule fois au passé) / "wenn" = quand (répétition, présent, futur)'),
(94, 'Ils sont interchangeables', FALSE, 'Non — als et wenn ont des usages distincts et non interchangeables'),
(94, '"Wenn" pour le passé uniquement', FALSE, 'C''est l''inverse : "als" pour le passé unique'),
(94, '"Als" est plus formel que "wenn"', FALSE, 'Ce n''est pas une question de formalité mais de temporalité'),
(95, 'Ich möchte, dass du kommst', TRUE, '"Ich möchte, dass du kommst" — dass + verbe en fin, subjonctif non requis'),
(95, 'Ich möchte du kommst', FALSE, 'Il manque la conjonction "dass"'),
(95, 'Ich will, du zu kommen', FALSE, 'Construction incorrecte — zu + infinitif n''est possible qu''avec même sujet'),
(95, 'Ich wünsche dich kommen', FALSE, 'Construction incorrecte en allemand'),
(96, 'dem alten Mann', TRUE, '"helfen" régit le datif → "dem alten Mann" (vieux monsieur au datif)'),
(96, 'den alten Mann', FALSE, '"den" est l''accusatif — helfen prend le datif'),
(96, 'der alte Mann', FALSE, '"der alte Mann" est le nominatif (sujet)'),
(96, 'des alten Mannes', FALSE, '"des alten Mannes" est le génitif');

-- ===== QCMs B2, C1, C2 (simplifiés pour la clarté) =====

INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
-- QCM 10 B2 Société
(10, '"Einerseits... andererseits..." structure exprime...', 'SINGLE_CHOICE', 1),
(10, 'Quel terme désigne l''intégration des immigrés en allemand ?', 'SINGLE_CHOICE', 2),
(10, '"Die Digitalisierung" fait référence à...', 'SINGLE_CHOICE', 3),
(10, 'Comment nominalise-t-on "entscheiden" (décider) ?', 'SINGLE_CHOICE', 4),
(10, '"Es ist bekannt, dass..." introduit...', 'SINGLE_CHOICE', 5);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(97, 'Une opposition / nuance entre deux aspects', TRUE, '"Einerseits...andererseits" = D''un côté...de l''autre côté — structure d''opposition'),
(97, 'Une énumération chronologique', FALSE, 'L''énumération temporelle utilise "zunächst...dann...schließlich"'),
(97, 'Une cause et conséquence', FALSE, 'Cause/conséquence : "weil...deshalb" ou "da...daher"'),
(97, 'Une condition', FALSE, 'La condition utilise "wenn...dann"'),
(98, 'die Integration', TRUE, 'die Integration = l''intégration (des personnes issues de l''immigration)'),
(98, 'die Migration', FALSE, 'die Migration = la migration (le mouvement, pas l''intégration)'),
(98, 'die Assimilation', FALSE, 'die Assimilation = l''assimilation (terme plus radical qu''intégration)'),
(98, 'die Einbürgerung', FALSE, 'die Einbürgerung = la naturalisation (obtention de la nationalité)'),
(99, 'La numérisation / transformation numérique', TRUE, 'die Digitalisierung = la numérisation, la transformation digitale de la société'),
(99, 'La mondialisation', FALSE, 'La mondialisation = die Globalisierung'),
(99, 'L''informatique', FALSE, 'L''informatique = die Informatik'),
(99, 'Les réseaux sociaux', FALSE, 'Les réseaux sociaux = die sozialen Netzwerke'),
(100, 'die Entscheidung', TRUE, 'entscheiden → die Entscheidung (la décision) — nominalisation standard'),
(100, 'das Entscheiden', FALSE, '"das Entscheiden" est une gérondivisation (infinitif substantivé), moins courant'),
(100, 'der Entscheid', FALSE, '"der Entscheid" existe mais est moins standard que "die Entscheidung"'),
(100, 'die Entscheidsamkeit', FALSE, 'Ce mot n''existe pas'),
(101, 'Un fait généralement admis ou connu', TRUE, '"Es ist bekannt, dass..." = Il est connu que... — présente un fait établi'),
(101, 'Une opinion personnelle', FALSE, '"À mon avis" = Meiner Meinung nach / Ich finde, dass'),
(101, 'Une hypothèse', FALSE, '"On suppose que" = Es wird vermutet, dass / Man nimmt an, dass'),
(101, 'Une obligation', FALSE, '"Il faut que" = Es ist notwendig, dass / Man muss');

INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
-- QCM 11 B2 Économie
(11, '"Die Inflation" désigne...', 'SINGLE_CHOICE', 1),
(11, 'Quel terme signifie "faillite" en allemand ?', 'SINGLE_CHOICE', 2),
(11, '"Der Umsatz" en économie est...', 'SINGLE_CHOICE', 3),
(11, 'Comment dit-on "bourse des valeurs" ?', 'SINGLE_CHOICE', 4),
(11, '"Gemäß dem Bericht" signifie...', 'SINGLE_CHOICE', 5);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(102, 'La hausse générale des prix', TRUE, 'die Inflation = hausse générale et continue des prix'),
(102, 'La récession économique', FALSE, 'La récession = die Rezession'),
(102, 'Le taux de chômage', FALSE, 'Le taux de chômage = die Arbeitslosenquote'),
(102, 'Le déficit budgétaire', FALSE, 'Le déficit = das Haushaltsdefizit'),
(103, 'der Bankrott / die Insolvenz', TRUE, '"Bankrott" et "Insolvenz" désignent tous deux la faillite'),
(103, 'der Gewinn', FALSE, '"der Gewinn" = le bénéfice/profit'),
(103, 'die Schulden', FALSE, '"die Schulden" = les dettes'),
(103, 'der Verlust', FALSE, '"der Verlust" = la perte'),
(104, 'Le chiffre d''affaires', TRUE, '"der Umsatz" = le chiffre d''affaires (total des ventes)'),
(104, 'Le bénéfice net', FALSE, '"Le bénéfice net" = der Gewinn / der Nettogewinn'),
(104, 'Les coûts de production', FALSE, '"Les coûts" = die Kosten / die Produktionskosten'),
(104, 'Le capital', FALSE, '"Le capital" = das Kapital'),
(105, 'die Börse', TRUE, 'die Börse = la bourse des valeurs (Frankfurter Börse, etc.)'),
(105, 'die Bank', FALSE, 'die Bank = la banque'),
(105, 'das Finanzamt', FALSE, 'das Finanzamt = le fisc / administration fiscale'),
(105, 'die Versicherung', FALSE, 'die Versicherung = l''assurance'),
(106, 'Selon le rapport / Conformément au rapport', TRUE, '"Gemäß" = selon, conformément à — utilisé dans les textes formels'),
(106, 'Malgré le rapport', FALSE, '"Malgré" = trotz (+ génitif) / trotzdem'),
(106, 'À cause du rapport', FALSE, '"À cause de" = wegen (+ génitif)'),
(106, 'Sans le rapport', FALSE, '"Sans" = ohne (+ accusatif)');

INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
-- QCM 12 B2 Grammaire avancée
(12, 'Quelle est la forme du participe présent de "laufen" utilisé comme adjectif ?', 'SINGLE_CHOICE', 1),
(12, '"Anstatt zu arbeiten, schläft er." Cette structure exprime...', 'SINGLE_CHOICE', 2),
(12, 'Le Konjunktiv I sert principalement à...', 'SINGLE_CHOICE', 3),
(12, '"Das zu lösende Problem" signifie...', 'SINGLE_CHOICE', 4),
(12, 'Quelle est la différence entre "seit" et "vor" ?', 'SINGLE_CHOICE', 5);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(107, 'laufend', TRUE, '"laufend" (courant) est le participe présent de laufen — ex: fließendes Wasser'),
(107, 'gelaufen', FALSE, '"gelaufen" est le participe passé'),
(107, 'läuft', FALSE, '"läuft" est la 3ème personne du présent'),
(107, 'laufig', FALSE, '"laufig" n''existe pas en allemand'),
(108, 'Une substitution / alternative négative (au lieu de)', TRUE, '"Anstatt zu..." = au lieu de faire qqch — indique une substitution'),
(108, 'Un but', FALSE, 'Le but s''exprime avec "um...zu"'),
(108, 'Une condition', FALSE, 'La condition s''exprime avec "wenn" ou "falls"'),
(108, 'Une cause', FALSE, 'La cause s''exprime avec "weil" ou "da"'),
(109, 'Rapporter des paroles (discours indirect)', TRUE, 'Konjunktiv I est la forme canonique du discours indirect en allemand écrit'),
(109, 'Exprimer la politesse', FALSE, 'La politesse utilise le Konjunktiv II (hätte, würde, könnte...)'),
(109, 'Formuler des hypothèses', FALSE, 'Les hypothèses utilisent le Konjunktiv II'),
(109, 'Décrire le passé', FALSE, 'Le passé utilise Präteritum ou Perfekt'),
(110, 'Le problème à résoudre', TRUE, '"Das zu lösende Problem" = le problème qui doit être résolu (participe futur passif)'),
(110, 'Le problème résolu', FALSE, '"Le problème résolu" = das gelöste Problem'),
(110, 'Le problème résolvable', FALSE, '"Résolvable" = das lösbare Problem'),
(110, 'Le problème en cours de résolution', FALSE, '"En cours de résolution" = das gerade gelöste Problem'),
(111, '"Seit" avec le présent pour durée en cours, "vor" pour moment passé révolu', TRUE, '"Seit 3 Jahren lerne ich Deutsch" (depuis) vs "Vor 3 Jahren" (il y a 3 ans)'),
(111, 'Ils sont synonymes', FALSE, '"Seit" et "vor" ne sont jamais synonymes'),
(111, '"Vor" pour durée en cours', FALSE, 'C''est "seit" qui exprime la durée en cours'),
(111, '"Seit" pour moment passé révolu', FALSE, '"Vor" exprime le moment passé révolu, pas "seit"');

INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
-- QCM 13 C1 Langue académique
(13, 'Quel terme académique signifie "hypothèse" ?', 'SINGLE_CHOICE', 1),
(13, '"Darüber hinaus" dans un texte académique signifie...', 'SINGLE_CHOICE', 2),
(13, 'Comment cite-t-on une source en allemand académique ?', 'SINGLE_CHOICE', 3),
(13, '"Die vorliegende Arbeit untersucht..." signifie...', 'SINGLE_CHOICE', 4),
(13, 'Quel registre convient à un exposé universitaire ?', 'SINGLE_CHOICE', 5);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(112, 'die Hypothese', TRUE, 'die Hypothese = l''hypothèse (terme scientifique direct)'),
(112, 'die Annahme', FALSE, 'die Annahme = l''hypothèse/supposition (plus général, moins technique)'),
(112, 'die Vermutung', FALSE, 'die Vermutung = la supposition (plus courant, moins académique)'),
(112, 'die Theorie', FALSE, 'die Theorie = la théorie (plus établie qu''une hypothèse)'),
(113, 'De plus / En outre', TRUE, '"Darüber hinaus" = de plus, en outre — connecteur additif formel'),
(113, 'Cependant', FALSE, '"Cependant" = jedoch / allerdings'),
(113, 'Par conséquent', FALSE, '"Par conséquent" = folglich / daher / deshalb'),
(113, 'En revanche', FALSE, '"En revanche" = dagegen / hingegen'),
(114, 'laut + Nom au Dativ ou "Nach X (Jahr)"', TRUE, 'Ex: "Laut Müller (2020)..." ou "Nach Chomsky..." — conventions académiques allemandes'),
(114, 'avec guillemets seulement', FALSE, 'Les guillemets seuls ne suffisent pas — il faut indiquer la source'),
(114, 'en bas de page uniquement', FALSE, 'Les notes de bas de page existent mais ne remplacent pas la citation dans le texte'),
(114, 'sans indication de source', FALSE, 'Tout emprunt doit être sourcé en contexte académique'),
(115, 'Le présent travail examine / Cette étude porte sur...', TRUE, '"Die vorliegende Arbeit untersucht..." = formule académique pour présenter son travail'),
(115, 'Je travaille maintenant sur...', FALSE, 'La 1ère personne est à éviter dans l''écriture académique allemande formelle'),
(115, 'Ce problème est difficile', FALSE, 'Cela se dirait "Dieses Problem ist schwierig"'),
(115, 'Nous avons trouvé que...', FALSE, 'Cela se dirait "Es wurde festgestellt, dass..."'),
(116, 'Sachlich-neutraler Stil (style neutre et objectif)', TRUE, 'L''exposé universitaire demande un style neutre, impersonnel et factuel'),
(116, 'Umgangssprache (langue familière)', FALSE, 'La langue familière est inadaptée au contexte universitaire'),
(116, 'Poetischer Stil (style poétique)', FALSE, 'Le style poétique n''est pas adapté aux exposés scientifiques'),
(116, 'Mündlicher Erzählstil (style narratif oral)', FALSE, 'Le style narratif oral manque de rigueur académique');

INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
-- QCM 14 C1 Stylistique
(14, 'Qu''est-ce qu''une "Redewendung" ?', 'SINGLE_CHOICE', 1),
(14, '"Jemanden auf den Arm nehmen" signifie...', 'SINGLE_CHOICE', 2),
(14, 'Quel procédé est utilisé dans "Die Blätter flüstern im Wind" ?', 'SINGLE_CHOICE', 3),
(14, '"Es war einmal..." est une formule de...', 'SINGLE_CHOICE', 4),
(14, 'Qu''est-ce que le Konjunktiv I exprime dans "Er sagte, er sei krank" ?', 'SINGLE_CHOICE', 5);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(117, 'Une expression idiomatique figée', TRUE, '"Redewendung" = locution / expression idiomatique dont le sens est non littéral'),
(117, 'Un proverbe', FALSE, 'Un proverbe = ein Sprichwort (sagesse populaire sous forme de phrase)'),
(117, 'Une métaphore', FALSE, 'Une métaphore = eine Metapher (comparaison implicite)'),
(117, 'Un euphémisme', FALSE, 'Un euphémisme = ein Euphemismus (atténuation d''un terme fort)'),
(118, 'Se moquer de quelqu''un / le faire marcher', TRUE, '"Jemanden auf den Arm nehmen" = se payer la tête de quelqu''un / faire marcher'),
(118, 'Aider quelqu''un physiquement', FALSE, 'Le sens littéral (prendre dans les bras) est ici non pertinent'),
(118, 'Battre quelqu''un', FALSE, 'Battre = jemanden schlagen'),
(118, 'Inviter quelqu''un', FALSE, 'Inviter = jemanden einladen'),
(119, 'La personnification', TRUE, 'Les feuilles qui "chuchotent" — attribuer une action humaine à la nature'),
(119, 'L''hyperbole', FALSE, 'L''hyperbole = die Hyperbel (exagération forte)'),
(119, 'L''allitération', FALSE, 'L''allitération = die Alliteration (répétition de son initial)'),
(119, 'L''antithèse', FALSE, 'L''antithèse = die Antithese (opposition de deux idées)'),
(120, 'Conte / début de conte de fées', TRUE, '"Es war einmal..." = Il était une fois... — formule d''ouverture des contes allemands'),
(120, 'Discours politique', FALSE, 'Les discours politiques commencent par "Meine Damen und Herren..."'),
(120, 'Article scientifique', FALSE, 'Les articles scientifiques commencent par une introduction objective'),
(120, 'Lettre formelle', FALSE, 'Les lettres formelles commencent par "Sehr geehrte/r..."'),
(121, 'Le discours indirect (rapporter les paroles de quelqu''un)', TRUE, '"Er sagte, er sei krank" — Konjunktiv I rapporte les paroles sans les valider'),
(121, 'Un souhait', FALSE, 'Le souhait utilise plutôt Konjunktiv II : "Wenn er doch käme!"'),
(121, 'Une hypothèse', FALSE, 'L''hypothèse utilise Konjunktiv II : "Wenn er krank wäre..."'),
(121, 'Une politesse', FALSE, 'La politesse utilise Konjunktiv II : "Könnten Sie bitte..."');

INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
-- QCM 15 C1 Rhétorique et culture
(15, 'Quel philosophe a écrit "Kritik der reinen Vernunft" ?', 'SINGLE_CHOICE', 1),
(15, '"Das Leben der Anderen" est...', 'SINGLE_CHOICE', 2),
(15, 'Comment dit-on "prendre la parole" dans un débat formel ?', 'SINGLE_CHOICE', 3),
(15, 'Le dialecte bavarois ("Bairisch") est parlé dans...', 'SINGLE_CHOICE', 4),
(15, '"Hochdeutsch" désigne...', 'SINGLE_CHOICE', 5);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(122, 'Emmanuel Kant', TRUE, '"Kritik der reinen Vernunft" (1781) est l''œuvre majeure d''Immanuel Kant'),
(122, 'Friedrich Hegel', FALSE, 'Hegel est connu pour la "Phänomenologie des Geistes"'),
(122, 'Friedrich Nietzsche', FALSE, 'Nietzsche est connu pour "Also sprach Zarathustra" et "Jenseits von Gut und Böse"'),
(122, 'Martin Heidegger', FALSE, 'Heidegger est connu pour "Sein und Zeit"'),
(123, 'Un film oscarisé sur la surveillance en RDA', TRUE, '"Das Leben der Anderen" (2006) — film sur la Stasi en RDA, Oscar du meilleur film étranger'),
(123, 'Un roman de Kafka', FALSE, 'Kafka n''a pas écrit "Das Leben der Anderen"'),
(123, 'Une pièce de Brecht', FALSE, 'Brecht est connu pour "Der kaukasische Kreidekreis" et "Dreigroschenoper"'),
(123, 'Un opéra de Wagner', FALSE, 'Wagner est connu pour la "Ring des Nibelungen" et "Tristan und Isolde"'),
(124, 'Das Wort ergreifen', TRUE, '"Das Wort ergreifen" = prendre la parole (dans un contexte formel)'),
(124, 'Sprechen', FALSE, '"Sprechen" = parler — trop générique pour "prendre la parole" formellement'),
(124, 'Reden', FALSE, '"Reden" = parler / tenir un discours — correct mais moins précis pour "prendre la parole"'),
(124, 'Sagen', FALSE, '"Sagen" = dire — pas l''expression idiomatique pour "prendre la parole"'),
(125, 'Bavaria (Bayern) et Autriche', TRUE, 'Le Bairisch est parlé en Bavière et en grande partie de l''Autriche'),
(125, 'Berlin et Hambourg', FALSE, 'Berlin et Hambourg ont leurs propres dialectes (Berlinerisch, Hamburgisch)'),
(125, 'La Suisse alémanique', FALSE, 'La Suisse alémanique parle l''Alémanique (Alemannisch), pas le Bairisch'),
(125, 'La Saxe', FALSE, 'La Saxe a son propre dialecte : das Sächsische'),
(126, 'L''allemand standard (langue cultivée commune)', TRUE, '"Hochdeutsch" = l''allemand standard, normé, enseigné et compris partout'),
(126, 'L''allemand du nord uniquement', FALSE, 'C''est une idée reçue — Hochdeutsch est supra-régional'),
(126, 'L''allemand suisse', FALSE, 'Le Schweizerdeutsch est un dialecte distinct du Hochdeutsch'),
(126, 'L''allemand médiéval', FALSE, 'L''allemand médiéval = Mittelhochdeutsch');

INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
-- QCM 16 C2 Maîtrise complète
(16, 'Quel est le subjonctif I de "wissen" à la 3ème personne ?', 'SINGLE_CHOICE', 1),
(16, '"Unbeschadet dessen" dans un texte juridique signifie...', 'SINGLE_CHOICE', 2),
(16, 'Qu''est-ce que le "Genitivus subjectivus" ?', 'SINGLE_CHOICE', 3),
(16, '"Die Sprache ist das Haus des Seins" est une citation de...', 'SINGLE_CHOICE', 4),
(16, 'Quel cas régit la préposition "laut" dans un usage formel ?', 'SINGLE_CHOICE', 5);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(127, 'wisse', TRUE, 'Konjunktiv I de wissen : er/sie/es wisse (utilisé dans le discours indirect)'),
(127, 'weiß', FALSE, '"weiß" est l''indicatif présent'),
(127, 'wusste', FALSE, '"wusste" est le Präteritum indicatif'),
(127, 'gewusst', FALSE, '"gewusst" est le participe passé'),
(128, 'Sans préjudice de cela / Nonobstant', TRUE, '"Unbeschadet dessen" = nonobstant / sans préjudice — terme juridique formel'),
(128, 'À cause de cela', FALSE, '"À cause de cela" = deswegen / daher'),
(128, 'Malgré cela', FALSE, '"Malgré cela" = trotzdem / dennoch'),
(128, 'En raison de cela', FALSE, '"En raison de cela" = aufgrund dessen'),
(129, 'Génitif dont le nom représente le sujet de l''action', TRUE, '"Der Gesang der Vögel" (le chant des oiseaux = les oiseaux chantent) — génitif subjectif'),
(129, 'Génitif dont le nom représente l''objet de l''action', FALSE, 'Cela est le "Genitivus objectivus" : "die Liebe zu Gott" (l''amour de Dieu = on aime Dieu)'),
(129, 'Génitif de possession simple', FALSE, 'Possession simple = "das Buch des Mannes" (le livre de l''homme)'),
(129, 'Génitif partitif', FALSE, 'Génitif partitif = "ein Glas Weines" (un verre de vin)'),
(130, 'Martin Heidegger', TRUE, '"Die Sprache ist das Haus des Seins" — Heidegger, "Brief über den Humanismus" (1946)'),
(130, 'Ludwig Wittgenstein', FALSE, 'Wittgenstein a dit : "Die Grenzen meiner Sprache bedeuten die Grenzen meiner Welt"'),
(130, 'Wilhelm von Humboldt', FALSE, 'Humboldt : "Die Sprache ist das bildende Organ des Gedankens"'),
(130, 'Friedrich de Saussure', FALSE, 'De Saussure était suisse, il écrivait en français, pas en allemand'),
(131, 'Le génitif ou datif selon contexte', TRUE, '"Laut" + génitif (formel écrit) ou + datif (usage courant) — les deux acceptés'),
(131, 'L''accusatif uniquement', FALSE, '"Laut" ne régit jamais l''accusatif'),
(131, 'Le nominatif uniquement', FALSE, '"Laut" ne régit jamais le nominatif'),
(131, 'Le datif uniquement', FALSE, '"Laut" accepte aussi le génitif en usage formel');

INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
-- QCM 17 C2 Littérature
(17, 'Dans "Die Verwandlung" de Kafka, Gregor Samsa se transforme en...', 'SINGLE_CHOICE', 1),
(17, 'Quel est le thème central du "Faust" de Goethe ?', 'SINGLE_CHOICE', 2),
(17, 'L''expressionnisme allemand en littérature correspond à...', 'SINGLE_CHOICE', 3),
(17, '"Der Vorleser" de Bernhard Schlink traite de...', 'SINGLE_CHOICE', 4),
(17, 'Quel écrivain est associé au "Nouveau Roman" allemand (Gruppe 47) ?', 'SINGLE_CHOICE', 5);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(132, 'Un insecte gigantesque / une vermine', TRUE, '"Er fand sich in ein ungeheueres Ungeziefer verwandelt" — Kafka, 1915'),
(132, 'Un loup', FALSE, 'Le loup est dans "Steppenwolf" de Hermann Hesse (1927)'),
(132, 'Un oiseau', FALSE, 'Gregor Samsa ne se transforme pas en oiseau'),
(132, 'Un fantôme', FALSE, 'La transformation en fantôme n''est pas le propos de Kafka'),
(133, 'La quête du savoir absolu et le pacte avec le diable', TRUE, 'Faust vend son âme à Méphistophélès pour obtenir la connaissance ultime et le bonheur'),
(133, 'L''amour romantique impossible', FALSE, 'L''amour romantique est un thème secondaire (Margarete), pas central'),
(133, 'La révolution politique', FALSE, 'Goethe n''avait pas une visée politique révolutionnaire avec Faust'),
(133, 'La critique de la religion', FALSE, 'La religion est présente mais secondaire par rapport au thème du savoir'),
(134, 'Début XXème siècle (1910-1925) — rupture et cri intérieur', TRUE, 'L''expressionnisme allemand : Trakl, Heym, Benn — 1910-1925, crise intérieure, déformation'),
(134, 'XVIIIème siècle — raison et lumières', FALSE, 'XVIIIème = Aufklärung (Lumières) : Lessing, Kant'),
(134, 'XIXème siècle — réalisme bourgeois', FALSE, 'XIXème = Realismus : Fontane, Keller, Storm'),
(134, 'Après 1945 — culpabilité et reconstruction', FALSE, 'Après 1945 = Trümmerliteratur, Gruppe 47 : Böll, Grass'),
(135, 'La culpabilité collective et la mémoire de l''Holocauste', TRUE, '"Der Vorleser" (1995) — relation entre un jeune homme et une ancienne gardienne de camp'),
(135, 'L''amour impossible au XIXème siècle', FALSE, 'Le roman se passe dans l''Allemagne d''après-guerre, pas au XIXème'),
(135, 'La réunification allemande', FALSE, 'La réunification est le thème d''autres œuvres, pas de "Der Vorleser"'),
(135, 'L''immigration et l''identité', FALSE, 'L''immigration n''est pas le sujet de "Der Vorleser"'),
(136, 'Heinrich Böll et Günter Grass', TRUE, 'La Gruppe 47 inclut Böll ("Billard um halbzehn") et Grass ("Die Blechtrommel")'),
(136, 'Goethe et Schiller', FALSE, 'Goethe et Schiller appartiennent au Classicisme (XVIIIème-début XIXème)'),
(136, 'Kafka et Rilke', FALSE, 'Kafka et Rilke sont liés au modernisme/expressionnisme, pas à la Gruppe 47'),
(136, 'Brecht et Benjamin', FALSE, 'Brecht et Benjamin appartiennent à la République de Weimar et à l''exil antinazi');

INSERT INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
-- QCM 18 C2 Philosophie
(18, '"Der Wille zur Macht" est un concept de...', 'SINGLE_CHOICE', 1),
(18, 'La "Dialektik" hégélienne se décompose en...', 'SINGLE_CHOICE', 2),
(18, '"Dasein" chez Heidegger désigne...', 'SINGLE_CHOICE', 3),
(18, 'Le "kategorischer Imperativ" de Kant dit...', 'SINGLE_CHOICE', 4),
(18, 'Habermas est connu pour sa théorie de...', 'SINGLE_CHOICE', 5);

INSERT INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
(137, 'Friedrich Nietzsche', TRUE, '"Der Wille zur Macht" est un concept central de Nietzsche — force vitale créatrice'),
(137, 'Arthur Schopenhauer', FALSE, 'Schopenhauer a "Der Wille zum Leben" — la volonté de vivre'),
(137, 'Georg Hegel', FALSE, 'Hegel est connu pour la Dialectique et la Phénoménologie de l''Esprit'),
(137, 'Karl Marx', FALSE, 'Marx est connu pour le matérialisme historique et "Das Kapital"'),
(138, 'Thèse, Antithèse, Synthèse', TRUE, 'La dialectique hégélienne : une idée (thèse) génère son contraire (antithèse) puis une synthèse'),
(138, 'Cause, Effet, Résolution', FALSE, 'Ce sont des catégories de la causalité, pas de la dialectique'),
(138, 'Passé, Présent, Futur', FALSE, 'Cette tripartition est temporelle, pas dialectique'),
(138, 'Corps, Âme, Esprit', FALSE, 'Cette tripartition est anthropologique/théologique, pas la dialectique hégélienne'),
(139, 'L''être-là de l''homme / l''existence humaine concrète', TRUE, '"Dasein" chez Heidegger = l''être-là, l''existence humaine dans sa concrétude temporelle'),
(139, 'L''essence abstraite de Dieu', FALSE, 'Heidegger s''oppose à la métaphysique théologique classique'),
(139, 'La conscience collective', FALSE, 'La conscience collective est un concept durkheimien, sociologique'),
(139, 'La matière première', FALSE, 'La matière première est un concept marxiste ou physique'),
(140, '"Agis selon une maxime universalisable"', TRUE, '"Handle nur nach derjenigen Maxime, durch die du zugleich wollen kannst, dass sie ein allgemeines Gesetz werde"'),
(140, '"Le bonheur est le but de toute action"', FALSE, 'C''est l''eudémonisme aristotélicien, pas Kant'),
(140, '"L''homme est un loup pour l''homme"', FALSE, 'C''est Hobbes (Homo homini lupus)'),
(140, '"Je pense donc je suis"', FALSE, 'C''est Descartes (Cogito ergo sum)'),
(141, 'L''agir communicationnel (kommunikatives Handeln)', TRUE, 'Habermas développe la théorie de l''agir communicationnel — rationalité du dialogue et consensus'),
(141, 'La volonté de puissance', FALSE, 'La volonté de puissance est de Nietzsche'),
(141, 'La déconstruction', FALSE, 'La déconstruction est de Jacques Derrida (philosophe français)'),
(141, 'L''herméneutique de la méfiance', FALSE, 'L''herméneutique de la méfiance est de Paul Ricœur');
