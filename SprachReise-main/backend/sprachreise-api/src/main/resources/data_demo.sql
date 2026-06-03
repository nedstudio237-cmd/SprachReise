-- =====================================================
-- SprachReise - Données de démonstration
-- Mot de passe de tous les comptes test : Admin@1234
-- =====================================================

USE sprachreise;

-- =====================
-- FORMATEURS (5 users)
-- =====================
INSERT IGNORE INTO users (email, password_hash, first_name, last_name, phone, role, city, bio, email_verified, active) VALUES
('amara.nkomo@sprachreise.com',   '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2', 'Amara',  'Nkomo',  '+237 690 123 456', 'TRAINER', 'Yaoundé',     'Docteur en linguistique germanophone, 8 ans d''expérience.', TRUE, TRUE),
('fatou.diallo@sprachreise.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2', 'Fatou',  'Diallo', '+237 677 234 567', 'TRAINER', 'Douala',      'Certifiée Goethe-Institut. Spécialiste en allemand des affaires.', TRUE, TRUE),
('jean.mbogo@sprachreise.com',    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2', 'Jean',   'Mbogo',  '+237 655 345 678', 'TRAINER', 'Bafoussam',   'Master en langue et culture allemandes.', TRUE, TRUE),
('marie.essong@sprachreise.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2', 'Marie',  'Essong', '+237 699 456 789', 'TRAINER', 'Yaoundé',     'Interprète professionnelle FR-DE-EN. Formatrice certifiée.', TRUE, TRUE),
('paul.tagne@sprachreise.com',    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2', 'Paul',   'Tagne',  '+237 670 567 890', 'TRAINER', 'Kribi',       'Professeur certifié, spécialiste préparation examens Goethe.', TRUE, TRUE);

-- =====================
-- APPRENANTS (5 users)
-- =====================
INSERT IGNORE INTO users (email, password_hash, first_name, last_name, phone, role, city, email_verified, active) VALUES
('alice.fouda@gmail.com',   '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2', 'Alice',  'Fouda',  '+237 651 111 111', 'LEARNER', 'Yaoundé',     TRUE, TRUE),
('eric.kamdem@gmail.com',   '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2', 'Eric',   'Kamdem', '+237 652 222 222', 'LEARNER', 'Douala',      TRUE, TRUE),
('sophie.biya@gmail.com',   '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2', 'Sophie', 'Biya',   '+237 653 333 333', 'LEARNER', 'Bafoussam',   TRUE, TRUE),
('martin.ateba@gmail.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2', 'Martin', 'Ateba',  '+237 654 444 444', 'LEARNER', 'Garoua',      TRUE, TRUE),
('claire.ngono@gmail.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2', 'Claire', 'Ngono',  '+237 655 555 555', 'LEARNER', 'Ngaoundéré',  TRUE, TRUE);

-- ========================
-- PROFILS FORMATEURS (5)
-- ========================
INSERT IGNORE INTO trainer_profiles (user_id, assigned_level_id, max_students, current_students, rating_avg)
SELECT u.id, l.id, 30, 18, 4.80 FROM users u, levels l WHERE u.email = 'amara.nkomo@sprachreise.com'  AND l.code = 'B2';

INSERT IGNORE INTO trainer_profiles (user_id, assigned_level_id, max_students, current_students, rating_avg)
SELECT u.id, l.id, 25, 12, 4.60 FROM users u, levels l WHERE u.email = 'fatou.diallo@sprachreise.com' AND l.code = 'C1';

INSERT IGNORE INTO trainer_profiles (user_id, assigned_level_id, max_students, current_students, rating_avg)
SELECT u.id, l.id, 30, 22, 4.70 FROM users u, levels l WHERE u.email = 'jean.mbogo@sprachreise.com'   AND l.code = 'A2';

INSERT IGNORE INTO trainer_profiles (user_id, assigned_level_id, max_students, current_students, rating_avg)
SELECT u.id, l.id, 20, 9,  4.90 FROM users u, levels l WHERE u.email = 'marie.essong@sprachreise.com' AND l.code = 'B1';

INSERT IGNORE INTO trainer_profiles (user_id, assigned_level_id, max_students, current_students, rating_avg)
SELECT u.id, l.id, 30, 25, 4.75 FROM users u, levels l WHERE u.email = 'paul.tagne@sprachreise.com'   AND l.code = 'A1';

-- ========================
-- COURS (12 cours)
-- ========================
INSERT IGNORE INTO courses (trainer_id, level_id, title, description, theme, video_path, video_duration_sec, pdf_path, status) VALUES
-- A1 - Paul Tagne
((SELECT id FROM users WHERE email='paul.tagne@sprachreise.com'),   (SELECT id FROM levels WHERE code='A1'), 'Begrüßungen — Les salutations',         'Apprenez à saluer et vous présenter en allemand.', 'Communication', 'storage/videos/a1_cours1.mp4', 1200, 'storage/pdf/a1_cours1.pdf', 'PUBLISHED'),
((SELECT id FROM users WHERE email='paul.tagne@sprachreise.com'),   (SELECT id FROM levels WHERE code='A1'), 'Zahlen und Farben — Chiffres et couleurs','Maîtrisez les chiffres de 1 à 100 et les couleurs.','Vocabulaire',   'storage/videos/a1_cours2.mp4', 1500, 'storage/pdf/a1_cours2.pdf', 'PUBLISHED'),
((SELECT id FROM users WHERE email='paul.tagne@sprachreise.com'),   (SELECT id FROM levels WHERE code='A1'), 'Familie und Freunde — Famille et amis',  'Parler de sa famille et de ses proches.',            'Vocabulaire',   'storage/videos/a1_cours3.mp4', 1350, 'storage/pdf/a1_cours3.pdf', 'PUBLISHED'),

-- A2 - Jean Mbogo
((SELECT id FROM users WHERE email='jean.mbogo@sprachreise.com'),   (SELECT id FROM levels WHERE code='A2'), 'Im Restaurant — Au restaurant',          'Commander un repas, comprendre un menu en allemand.','Vie quotidienne','storage/videos/a2_cours1.mp4', 1800, 'storage/pdf/a2_cours1.pdf', 'PUBLISHED'),
((SELECT id FROM users WHERE email='jean.mbogo@sprachreise.com'),   (SELECT id FROM levels WHERE code='A2'), 'Einkaufen — Faire ses achats',           'Vocabulaire des commerces et transactions.','Vie quotidienne',         'storage/videos/a2_cours2.mp4', 1600, 'storage/pdf/a2_cours2.pdf', 'PUBLISHED'),
((SELECT id FROM users WHERE email='jean.mbogo@sprachreise.com'),   (SELECT id FROM levels WHERE code='A2'), 'Reisen — Voyager',                       'Réserver un hôtel, prendre les transports.','Voyages',                 'storage/videos/a2_cours3.mp4', 2100, 'storage/pdf/a2_cours3.pdf', 'PUBLISHED'),

-- B1 - Marie Essong
((SELECT id FROM users WHERE email='marie.essong@sprachreise.com'), (SELECT id FROM levels WHERE code='B1'), 'Arbeit und Beruf — Travail et métiers',  'Parler de sa profession et du monde du travail.','Professionnel',     'storage/videos/b1_cours1.mp4', 2400, 'storage/pdf/b1_cours1.pdf', 'PUBLISHED'),
((SELECT id FROM users WHERE email='marie.essong@sprachreise.com'), (SELECT id FROM levels WHERE code='B1'), 'Gesundheit — La santé',                  'Vocabulaire médical, décrire ses symptômes.','Santé',                  'storage/videos/b1_cours2.mp4', 2200, 'storage/pdf/b1_cours2.pdf', 'PUBLISHED'),

-- B2 - Amara Nkomo
((SELECT id FROM users WHERE email='amara.nkomo@sprachreise.com'),  (SELECT id FROM levels WHERE code='B2'), 'Kultur und Geschichte Deutschlands',     'Découverte de la culture et de l''histoire allemande.','Culture',      'storage/videos/b2_cours1.mp4', 3000, 'storage/pdf/b2_cours1.pdf', 'PUBLISHED'),
((SELECT id FROM users WHERE email='amara.nkomo@sprachreise.com'),  (SELECT id FROM levels WHERE code='B2'), 'Wirtschaft und Finanzen',                'L''allemand dans le monde des affaires.','Affaires',                   'storage/videos/b2_cours2.mp4', 2700, 'storage/pdf/b2_cours2.pdf', 'PUBLISHED'),

-- C1 - Fatou Diallo
((SELECT id FROM users WHERE email='fatou.diallo@sprachreise.com'), (SELECT id FROM levels WHERE code='C1'), 'Literatur und Medien',                   'Analyser des textes littéraires et des médias allemands.','Littérature','storage/videos/c1_cours1.mp4', 3600, 'storage/pdf/c1_cours1.pdf', 'PUBLISHED'),
((SELECT id FROM users WHERE email='fatou.diallo@sprachreise.com'), (SELECT id FROM levels WHERE code='C1'), 'Verhandeln und Präsentieren',            'Techniques de négociation et de présentation.','Affaires',             'storage/videos/c1_cours2.mp4', 3300, 'storage/pdf/c1_cours2.pdf', 'PUBLISHED');

-- ========================
-- ARTICLES (12 articles)
-- ========================
INSERT IGNORE INTO articles (level_id, title, theme, body_markdown, author_id, published) VALUES
((SELECT id FROM levels WHERE code='A1'), 'Le genre des noms en allemand',          'Grammaire',   '# Der, Die, Das\n\nEn allemand, chaque nom a un genre...', (SELECT id FROM users WHERE email='paul.tagne@sprachreise.com'),   TRUE),
((SELECT id FROM levels WHERE code='A1'), 'L''alphabet allemand expliqué',           'Phonétique',  '# Das Alphabet\n\nL''allemand compte 30 lettres...', (SELECT id FROM users WHERE email='paul.tagne@sprachreise.com'),   TRUE),
((SELECT id FROM levels WHERE code='A1'), 'Les verbes essentiels au présent',        'Grammaire',   '# Konjugation\n\nsein, haben, werden...', (SELECT id FROM users WHERE email='paul.tagne@sprachreise.com'),       TRUE),
((SELECT id FROM levels WHERE code='A2'), 'Les prépositions de lieu',               'Grammaire',   '# Präpositionen\n\nin, an, auf, unter, über...', (SELECT id FROM users WHERE email='jean.mbogo@sprachreise.com'),   TRUE),
((SELECT id FROM levels WHERE code='A2'), 'Le parfait — Passé composé allemand',    'Grammaire',   '# Das Perfekt\n\nFormation avec haben ou sein...', (SELECT id FROM users WHERE email='jean.mbogo@sprachreise.com'),   TRUE),
((SELECT id FROM levels WHERE code='A2'), 'Vocabulaire : les vêtements',            'Vocabulaire', '# Kleidung\n\ndie Hose, das Hemd, die Jacke...', (SELECT id FROM users WHERE email='jean.mbogo@sprachreise.com'),    TRUE),
((SELECT id FROM levels WHERE code='B1'), 'Le subjonctif II — Konjunktiv II',       'Grammaire',   '# Konjunktiv II\n\nPour exprimer le souhait, la politesse...', (SELECT id FROM users WHERE email='marie.essong@sprachreise.com'), TRUE),
((SELECT id FROM levels WHERE code='B1'), 'Les connecteurs logiques',               'Grammaire',   '# Konnektoren\n\nweil, obwohl, damit, sodass...', (SELECT id FROM users WHERE email='marie.essong@sprachreise.com'),  TRUE),
((SELECT id FROM levels WHERE code='B1'), 'Écrire un email professionnel',          'Rédaction',   '# Formelle E-Mail\n\nSehr geehrte Damen und Herren...', (SELECT id FROM users WHERE email='marie.essong@sprachreise.com'), TRUE),
((SELECT id FROM levels WHERE code='B2'), 'Comprendre les médias allemands',        'Culture',     '# Deutsche Medien\n\nDie Zeit, Spiegel, ARD...', (SELECT id FROM users WHERE email='amara.nkomo@sprachreise.com'),   TRUE),
((SELECT id FROM levels WHERE code='B2'), 'Le génitif et ses usages',               'Grammaire',   '# Genitiv\n\nUsages modernes du génitif...', (SELECT id FROM users WHERE email='amara.nkomo@sprachreise.com'),    TRUE),
((SELECT id FROM levels WHERE code='C1'), 'Rhétorique et argumentation en allemand','Expression',  '# Rhetorik\n\nTechniques d''argumentation avancées...', (SELECT id FROM users WHERE email='fatou.diallo@sprachreise.com'), TRUE);

-- ==========================
-- SESSIONS LIVE (8 sessions)
-- ==========================
INSERT IGNORE INTO streaming_sessions (trainer_id, level_id, title, description, scheduled_start, duration_minutes, agora_channel, status) VALUES
((SELECT id FROM users WHERE email='paul.tagne@sprachreise.com'),   (SELECT id FROM levels WHERE code='A1'), 'Live A1 — Conversation débutants',       'Pratique orale : se présenter et dialoguer.',    '2026-06-02 18:00:00', 60,  'sr_a1_live_001', 'SCHEDULED'),
((SELECT id FROM users WHERE email='paul.tagne@sprachreise.com'),   (SELECT id FROM levels WHERE code='A1'), 'Live A1 — Questions-réponses grammaire', 'Session Q&A sur le genre et les articles.',      '2026-06-09 18:00:00', 60,  'sr_a1_live_002', 'SCHEDULED'),
((SELECT id FROM users WHERE email='jean.mbogo@sprachreise.com'),   (SELECT id FROM levels WHERE code='A2'), 'Live A2 — Jeux de rôle au quotidien',    'Simulations : café, gare, pharmacie.',           '2026-06-03 19:00:00', 90,  'sr_a2_live_001', 'SCHEDULED'),
((SELECT id FROM users WHERE email='jean.mbogo@sprachreise.com'),   (SELECT id FROM levels WHERE code='A2'), 'Live A2 — Révision passé et futur',      'Révision complète des temps.',                   '2026-06-10 19:00:00', 90,  'sr_a2_live_002', 'SCHEDULED'),
((SELECT id FROM users WHERE email='marie.essong@sprachreise.com'), (SELECT id FROM levels WHERE code='B1'), 'Live B1 — Débat sur l''environnement',   'Exprimer son opinion en allemand.',              '2026-06-04 20:00:00', 120, 'sr_b1_live_001', 'SCHEDULED'),
((SELECT id FROM users WHERE email='amara.nkomo@sprachreise.com'),  (SELECT id FROM levels WHERE code='B2'), 'Live B2 — Analyse de texte littéraire',  'Lecture et discussion : Kafka, Die Verwandlung.','2026-06-05 20:00:00', 120, 'sr_b2_live_001', 'SCHEDULED'),
((SELECT id FROM users WHERE email='fatou.diallo@sprachreise.com'), (SELECT id FROM levels WHERE code='C1'), 'Live C1 — Préparation examen Goethe',    'Entraînement officiel : oral et écrit.',         '2026-06-06 18:30:00', 120, 'sr_c1_live_001', 'SCHEDULED'),
((SELECT id FROM users WHERE email='paul.tagne@sprachreise.com'),   (SELECT id FROM levels WHERE code='A1'), 'Live A1 — Session de rattrapage',        'Révision des chapitres 1 à 3.',                  '2026-05-28 17:00:00', 60,  'sr_a1_live_003', 'ENDED');

-- ========================
-- QCMs (3 quiz)
-- ========================
INSERT IGNORE INTO qcms (level_id, title, theme, created_by) VALUES
((SELECT id FROM levels WHERE code='A1'), 'QCM A1 — Les articles',      'Grammaire',  (SELECT id FROM users WHERE email='paul.tagne@sprachreise.com')),
((SELECT id FROM levels WHERE code='A1'), 'QCM A1 — Les salutations',   'Vocabulaire',(SELECT id FROM users WHERE email='paul.tagne@sprachreise.com')),
((SELECT id FROM levels WHERE code='A2'), 'QCM A2 — Le parfait',        'Grammaire',  (SELECT id FROM users WHERE email='jean.mbogo@sprachreise.com'));

-- Questions QCM 1 : Les articles
INSERT IGNORE INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
((SELECT id FROM qcms WHERE title='QCM A1 — Les articles'),      'Quel est l''article défini du mot "Tisch" (la table) ?',     'SINGLE_CHOICE', 1),
((SELECT id FROM qcms WHERE title='QCM A1 — Les articles'),      'Quel est l''article défini du mot "Frau" (la femme) ?',      'SINGLE_CHOICE', 2),
((SELECT id FROM qcms WHERE title='QCM A1 — Les articles'),      'Quel est l''article défini du mot "Kind" (l''enfant) ?',     'SINGLE_CHOICE', 3),
((SELECT id FROM qcms WHERE title='QCM A1 — Les articles'),      'Quelle phrase est correcte ?',                               'SINGLE_CHOICE', 4);

-- Questions QCM 2 : Les salutations
INSERT IGNORE INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
((SELECT id FROM qcms WHERE title='QCM A1 — Les salutations'),   'Comment dit-on "Bonjour" (formel) en allemand ?',             'SINGLE_CHOICE', 1),
((SELECT id FROM qcms WHERE title='QCM A1 — Les salutations'),   'Comment dit-on "Au revoir" en allemand ?',                   'SINGLE_CHOICE', 2),
((SELECT id FROM qcms WHERE title='QCM A1 — Les salutations'),   'Comment dit-on "Bonsoir" en allemand ?',                     'SINGLE_CHOICE', 3),
((SELECT id FROM qcms WHERE title='QCM A1 — Les salutations'),   'Que signifie "Wie geht es Ihnen ?" ?',                       'SINGLE_CHOICE', 4);

-- Questions QCM 3 : Le parfait
INSERT IGNORE INTO qcm_questions (qcm_id, question_text, question_type, order_index) VALUES
((SELECT id FROM qcms WHERE title='QCM A2 — Le parfait'),        'Quel auxiliaire utilise-t-on avec "fahren" au parfait ?',    'SINGLE_CHOICE', 1),
((SELECT id FROM qcms WHERE title='QCM A2 — Le parfait'),        'Quel est le participe passé de "essen" (manger) ?',          'SINGLE_CHOICE', 2),
((SELECT id FROM qcms WHERE title='QCM A2 — Le parfait'),        'Quelle phrase au parfait est correcte ?',                    'SINGLE_CHOICE', 3);

-- Choix QCM 1 - Q1 (article de Tisch)
INSERT IGNORE INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Tisch" (la table) ?'), 'der', TRUE,  'Tisch est masculin : der Tisch.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Tisch" (la table) ?'), 'die', FALSE, 'die est l''article féminin.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Tisch" (la table) ?'), 'das', FALSE, 'das est l''article neutre.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Tisch" (la table) ?'), 'ein', FALSE, 'ein est l''article indéfini masculin/neutre.');

-- Choix QCM 1 - Q2 (article de Frau)
INSERT IGNORE INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Frau" (la femme) ?'), 'der', FALSE, 'der est l''article masculin.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Frau" (la femme) ?'), 'die', TRUE,  'Frau est féminin : die Frau.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Frau" (la femme) ?'), 'das', FALSE, 'das est l''article neutre.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Frau" (la femme) ?'), 'den', FALSE, 'den est l''article masculin à l''accusatif.');

-- Choix QCM 1 - Q3 (article de Kind)
INSERT IGNORE INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Kind" (l''enfant) ?'), 'der', FALSE, 'der est l''article masculin.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Kind" (l''enfant) ?'), 'die', FALSE, 'die est l''article féminin.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Kind" (l''enfant) ?'), 'das', TRUE,  'Kind est neutre : das Kind.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est l''article défini du mot "Kind" (l''enfant) ?'), 'ein', FALSE, 'ein est l''article indéfini.');

-- Choix QCM 1 - Q4 (phrase correcte)
INSERT IGNORE INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
((SELECT id FROM qcm_questions WHERE question_text='Quelle phrase est correcte ?'), 'Das Buch ist interessant.',  TRUE,  'Buch est neutre, das est correct.'),
((SELECT id FROM qcm_questions WHERE question_text='Quelle phrase est correcte ?'), 'Der Buch ist interessant.', FALSE, 'Buch est neutre, pas masculin.'),
((SELECT id FROM qcm_questions WHERE question_text='Quelle phrase est correcte ?'), 'Die Buch ist interessant.', FALSE, 'Buch est neutre, pas féminin.'),
((SELECT id FROM qcm_questions WHERE question_text='Quelle phrase est correcte ?'), 'Ein Buch ist interessant.', FALSE, 'L''article indéfini change le sens ici.');

-- Choix QCM 2 - Q1 (Bonjour formel)
INSERT IGNORE INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Bonjour" (formel) en allemand ?'), 'Guten Tag',   TRUE,  'Guten Tag est le bonjour formel universel.'),
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Bonjour" (formel) en allemand ?'), 'Hallo',       FALSE, 'Hallo est informel.'),
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Bonjour" (formel) en allemand ?'), 'Tschüss',     FALSE, 'Tschüss signifie Au revoir.'),
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Bonjour" (formel) en allemand ?'), 'Guten Abend', FALSE, 'Guten Abend signifie Bonsoir.');

-- Choix QCM 2 - Q2 (Au revoir)
INSERT IGNORE INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Au revoir" en allemand ?'), 'Tschüss',     TRUE,  'Tschüss est la forme informelle d''Au revoir.'),
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Au revoir" en allemand ?'), 'Guten Tag',   FALSE, 'Guten Tag signifie Bonjour.'),
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Au revoir" en allemand ?'), 'Bitte',       FALSE, 'Bitte signifie S''il vous plaît.'),
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Au revoir" en allemand ?'), 'Danke',       FALSE, 'Danke signifie Merci.');

-- Choix QCM 2 - Q3 (Bonsoir)
INSERT IGNORE INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Bonsoir" en allemand ?'), 'Guten Abend',  TRUE,  'Guten Abend est Bonsoir.'),
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Bonsoir" en allemand ?'), 'Guten Morgen', FALSE, 'Guten Morgen est Bonjour (le matin).'),
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Bonsoir" en allemand ?'), 'Gute Nacht',   FALSE, 'Gute Nacht est Bonne nuit.'),
((SELECT id FROM qcm_questions WHERE question_text='Comment dit-on "Bonsoir" en allemand ?'), 'Guten Tag',    FALSE, 'Guten Tag est Bonjour (journée).');

-- Choix QCM 2 - Q4 (Wie geht es Ihnen)
INSERT IGNORE INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
((SELECT id FROM qcm_questions WHERE question_text='Que signifie "Wie geht es Ihnen ?" ?'), 'Comment allez-vous ?',   TRUE,  'Forme polie de demander comment va quelqu''un.'),
((SELECT id FROM qcm_questions WHERE question_text='Que signifie "Wie geht es Ihnen ?" ?'), 'D''où venez-vous ?',     FALSE, 'D''où venez-vous = Woher kommen Sie ?'),
((SELECT id FROM qcm_questions WHERE question_text='Que signifie "Wie geht es Ihnen ?" ?'), 'Comment vous appelez-vous ?', FALSE, 'Comment vous appelez-vous = Wie heißen Sie ?'),
((SELECT id FROM qcm_questions WHERE question_text='Que signifie "Wie geht es Ihnen ?" ?'), 'Quel âge avez-vous ?',   FALSE, 'Quel âge avez-vous = Wie alt sind Sie ?');

-- Choix QCM 3 - Q1 (auxiliaire fahren)
INSERT IGNORE INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
((SELECT id FROM qcm_questions WHERE question_text='Quel auxiliaire utilise-t-on avec "fahren" au parfait ?'), 'sein',  TRUE,  'fahren indique un déplacement : auxiliaire sein.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel auxiliaire utilise-t-on avec "fahren" au parfait ?'), 'haben', FALSE, 'haben s''utilise avec la plupart des verbes transitifs.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel auxiliaire utilise-t-on avec "fahren" au parfait ?'), 'werden',FALSE, 'werden s''utilise pour le futur et le passif.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel auxiliaire utilise-t-on avec "fahren" au parfait ?'), 'dürfen',FALSE, 'dürfen est un verbe de modalité.');

-- Choix QCM 3 - Q2 (participe de essen)
INSERT IGNORE INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
((SELECT id FROM qcm_questions WHERE question_text='Quel est le participe passé de "essen" (manger) ?'), 'gegessen', TRUE,  'Le participe de essen est gegessen.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est le participe passé de "essen" (manger) ?'), 'geessen',  FALSE, 'Forme incorrecte.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est le participe passé de "essen" (manger) ?'), 'esste',    FALSE, 'Conjugaison incorrecte.'),
((SELECT id FROM qcm_questions WHERE question_text='Quel est le participe passé de "essen" (manger) ?'), 'gessen',   FALSE, 'Le préfixe ge- est requis : gegessen.');

-- Choix QCM 3 - Q3 (phrase parfait correcte)
INSERT IGNORE INTO qcm_choices (question_id, choice_text, is_correct, explanation) VALUES
((SELECT id FROM qcm_questions WHERE question_text='Quelle phrase au parfait est correcte ?'), 'Ich habe gegessen.',        TRUE,  'haben + participe passé : correct.'),
((SELECT id FROM qcm_questions WHERE question_text='Quelle phrase au parfait est correcte ?'), 'Ich gegessen habe.',        FALSE, 'L''auxiliaire doit être en 2ème position.'),
((SELECT id FROM qcm_questions WHERE question_text='Quelle phrase au parfait est correcte ?'), 'Ich habe essen.',           FALSE, 'Il faut le participe passé, pas l''infinitif.'),
((SELECT id FROM qcm_questions WHERE question_text='Quelle phrase au parfait est correcte ?'), 'Ich bin gegessen.',         FALSE, 'essen se conjugue avec haben, pas sein.');

-- ===========================
-- PROGRESSION APPRENANTS (5)
-- ===========================
INSERT IGNORE INTO learner_progress (learner_id, level_id, courses_completed, sessions_attended, qcm_avg_score, total_minutes, completion_percentage) VALUES
((SELECT id FROM users WHERE email='alice.fouda@gmail.com'),  (SELECT id FROM levels WHERE code='A1'), 2, 1, 78.50, 90,  45.00),
((SELECT id FROM users WHERE email='eric.kamdem@gmail.com'),  (SELECT id FROM levels WHERE code='A1'), 3, 2, 85.00, 130, 65.00),
((SELECT id FROM users WHERE email='sophie.biya@gmail.com'),  (SELECT id FROM levels WHERE code='A2'), 1, 0, 70.00, 45,  25.00),
((SELECT id FROM users WHERE email='martin.ateba@gmail.com'), (SELECT id FROM levels WHERE code='A1'), 3, 3, 92.00, 160, 80.00),
((SELECT id FROM users WHERE email='claire.ngono@gmail.com'), (SELECT id FROM levels WHERE code='A1'), 1, 1, 65.00, 50,  30.00);

-- ==========================
-- ABONNEMENTS (5)
-- ==========================
INSERT IGNORE INTO subscriptions (learner_id, plan_id, status, started_at, expires_at, auto_renew) VALUES
((SELECT id FROM users WHERE email='alice.fouda@gmail.com'),  (SELECT id FROM subscription_plans WHERE code='STANDARD'), 'ACTIVE',  '2026-05-01 00:00:00', '2026-06-01 00:00:00', TRUE),
((SELECT id FROM users WHERE email='eric.kamdem@gmail.com'),  (SELECT id FROM subscription_plans WHERE code='PREMIUM'),  'ACTIVE',  '2026-05-01 00:00:00', '2026-06-01 00:00:00', TRUE),
((SELECT id FROM users WHERE email='sophie.biya@gmail.com'),  (SELECT id FROM subscription_plans WHERE code='BASIC'),   'ACTIVE',  '2026-05-10 00:00:00', '2026-06-10 00:00:00', FALSE),
((SELECT id FROM users WHERE email='martin.ateba@gmail.com'), (SELECT id FROM subscription_plans WHERE code='PREMIUM'), 'ACTIVE',  '2026-04-15 00:00:00', '2026-05-15 00:00:00', TRUE),
((SELECT id FROM users WHERE email='claire.ngono@gmail.com'), (SELECT id FROM subscription_plans WHERE code='BASIC'),   'EXPIRED', '2026-04-01 00:00:00', '2026-05-01 00:00:00', FALSE);

-- ==========================
-- PAIEMENTS (5)
-- ==========================
INSERT IGNORE INTO payments (subscription_id, amount_fcfa, method, status, external_transaction_id, paid_at)
SELECT s.id, 7500,  'MTN_MOMO',     'SUCCESS', 'MTN_20260501_001', '2026-05-01 10:23:00' FROM subscriptions s JOIN users u ON s.learner_id=u.id WHERE u.email='alice.fouda@gmail.com'  LIMIT 1;
INSERT IGNORE INTO payments (subscription_id, amount_fcfa, method, status, external_transaction_id, paid_at)
SELECT s.id, 15000, 'ORANGE_MONEY', 'SUCCESS', 'OM_20260501_002',  '2026-05-01 11:05:00' FROM subscriptions s JOIN users u ON s.learner_id=u.id WHERE u.email='eric.kamdem@gmail.com'  LIMIT 1;
INSERT IGNORE INTO payments (subscription_id, amount_fcfa, method, status, external_transaction_id, paid_at)
SELECT s.id, 3000,  'MTN_MOMO',     'SUCCESS', 'MTN_20260510_003', '2026-05-10 09:14:00' FROM subscriptions s JOIN users u ON s.learner_id=u.id WHERE u.email='sophie.biya@gmail.com'  LIMIT 1;
INSERT IGNORE INTO payments (subscription_id, amount_fcfa, method, status, external_transaction_id, paid_at)
SELECT s.id, 15000, 'STRIPE',       'SUCCESS', 'pi_3Abc123456789',  '2026-04-15 14:32:00' FROM subscriptions s JOIN users u ON s.learner_id=u.id WHERE u.email='martin.ateba@gmail.com' LIMIT 1;
INSERT IGNORE INTO payments (subscription_id, amount_fcfa, method, status, external_transaction_id, paid_at)
SELECT s.id, 3000,  'ORANGE_MONEY', 'SUCCESS', 'OM_20260401_005',  '2026-04-01 16:00:00' FROM subscriptions s JOIN users u ON s.learner_id=u.id WHERE u.email='claire.ngono@gmail.com' LIMIT 1;
