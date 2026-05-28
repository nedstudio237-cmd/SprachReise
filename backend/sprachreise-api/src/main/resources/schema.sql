-- SprachReise - Schema base de donnees MySQL
-- A executer dans phpMyAdmin ou MySQL CLI

CREATE DATABASE IF NOT EXISTS sprachreise CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sprachreise;

-- LANGUES
CREATE TABLE IF NOT EXISTS languages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
INSERT INTO languages (code, name, active) VALUES ('de', 'Allemand', TRUE) ON DUPLICATE KEY UPDATE name='Allemand';

-- NIVEAUX CECRL
CREATE TABLE IF NOT EXISTS levels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    language_id BIGINT NOT NULL,
    code VARCHAR(5) NOT NULL,
    description TEXT,
    order_index INT NOT NULL,
    FOREIGN KEY (language_id) REFERENCES languages(id),
    UNIQUE KEY uk_language_code (language_id, code)
);
INSERT INTO levels (language_id, code, description, order_index) VALUES
    (1, 'A1', 'Niveau débutant', 1),
    (1, 'A2', 'Niveau élémentaire', 2),
    (1, 'B1', 'Niveau intermédiaire', 3),
    (1, 'B2', 'Niveau intermédiaire avancé', 4),
    (1, 'C1', 'Niveau avancé', 5),
    (1, 'C2', 'Niveau maîtrise', 6)
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- UTILISATEURS
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role ENUM('LEARNER','TRAINER','ADMIN') NOT NULL DEFAULT 'LEARNER',
    photo_url TEXT,
    city VARCHAR(100),
    birth_date DATE,
    bio TEXT,
    email_verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- CANDIDATURES FORMATEURS
CREATE TABLE IF NOT EXISTS trainer_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    requested_level_id BIGINT NOT NULL,
    status ENUM('PENDING','APPROVED','REJECTED','SUSPENDED') NOT NULL DEFAULT 'PENDING',
    diploma_pdf_path TEXT NOT NULL,
    motivation TEXT,
    native_language VARCHAR(100),
    reviewed_by BIGINT,
    review_motif TEXT,
    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (requested_level_id) REFERENCES levels(id),
    FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

-- PROFIL FORMATEUR VALIDE
CREATE TABLE IF NOT EXISTS trainer_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    assigned_level_id BIGINT NOT NULL,
    max_students INT NOT NULL DEFAULT 30,
    current_students INT NOT NULL DEFAULT 0,
    rating_avg DECIMAL(3,2) DEFAULT 0.00,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (assigned_level_id) REFERENCES levels(id)
);

-- INVITATIONS FORMATEURS
CREATE TABLE IF NOT EXISTS trainer_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    sent_by BIGINT NOT NULL,
    status ENUM('SENT','OPENED','REGISTERED','EXPIRED') DEFAULT 'SENT',
    expires_at DATETIME NOT NULL,
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sent_by) REFERENCES users(id)
);

-- COURS
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trainer_id BIGINT NOT NULL,
    level_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    theme VARCHAR(100),
    video_path TEXT NOT NULL,
    video_duration_sec INT,
    pdf_path TEXT NOT NULL,
    pdf_size_bytes BIGINT,
    status ENUM('DRAFT','PUBLISHED','REMOVED') DEFAULT 'DRAFT',
    publish_at DATETIME,
    view_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (trainer_id) REFERENCES users(id),
    FOREIGN KEY (level_id) REFERENCES levels(id)
);

-- ARTICLES ET CONTENUS TEXTUELS
CREATE TABLE IF NOT EXISTS articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    level_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    theme VARCHAR(100),
    body_markdown TEXT,
    pdf_path TEXT,
    author_id BIGINT,
    published BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (level_id) REFERENCES levels(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);

-- SESSIONS LIVE
CREATE TABLE IF NOT EXISTS streaming_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trainer_id BIGINT NOT NULL,
    level_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    scheduled_start DATETIME NOT NULL,
    duration_minutes INT NOT NULL,
    agora_channel VARCHAR(255) NOT NULL UNIQUE,
    status ENUM('SCHEDULED','LIVE','ENDED','CANCELLED') DEFAULT 'SCHEDULED',
    recording_path TEXT,
    attachment_pdf TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trainer_id) REFERENCES users(id),
    FOREIGN KEY (level_id) REFERENCES levels(id)
);

CREATE TABLE IF NOT EXISTS session_attendees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    learner_id BIGINT NOT NULL,
    registered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    joined_at DATETIME,
    left_at DATETIME,
    UNIQUE KEY uk_session_learner (session_id, learner_id),
    FOREIGN KEY (session_id) REFERENCES streaming_sessions(id),
    FOREIGN KEY (learner_id) REFERENCES users(id)
);

-- ABONNEMENTS
CREATE TABLE IF NOT EXISTS subscription_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code ENUM('BASIC','STANDARD','PREMIUM') NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    monthly_price_fcfa INT NOT NULL,
    features_json TEXT NOT NULL,
    active BOOLEAN DEFAULT TRUE
);
INSERT INTO subscription_plans (code, name, monthly_price_fcfa, features_json) VALUES
    ('BASIC', 'Apprenti', 3000, '{"cursus":true,"qcm":true,"pdf_read":true,"trainer_profiles":true,"video":false,"pdf_download":false,"live":false,"certificate":false}'),
    ('STANDARD', 'Voyageur', 7500, '{"cursus":true,"qcm":true,"pdf_read":true,"trainer_profiles":true,"video":true,"pdf_download":true,"live":false,"certificate":false}'),
    ('PREMIUM', 'Erudit', 15000, '{"cursus":true,"qcm":true,"pdf_read":true,"trainer_profiles":true,"video":true,"pdf_download":true,"live":true,"certificate":true}')
ON DUPLICATE KEY UPDATE name=VALUES(name);

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    learner_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status ENUM('ACTIVE','EXPIRED','CANCELLED','PENDING') NOT NULL,
    started_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    auto_renew BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (learner_id) REFERENCES users(id),
    FOREIGN KEY (plan_id) REFERENCES subscription_plans(id)
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    amount_fcfa INT NOT NULL,
    method ENUM('STRIPE','ORANGE_MONEY','MTN_MOMO') NOT NULL,
    status ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL,
    external_transaction_id VARCHAR(255),
    paid_at DATETIME,
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
);

-- CURSUS INTERNE (QCM)
CREATE TABLE IF NOT EXISTS qcms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    level_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    theme VARCHAR(100),
    created_by BIGINT,
    FOREIGN KEY (level_id) REFERENCES levels(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS qcm_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    qcm_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    question_type ENUM('SINGLE_CHOICE','MULTI_CHOICE') NOT NULL,
    order_index INT NOT NULL,
    FOREIGN KEY (qcm_id) REFERENCES qcms(id)
);

CREATE TABLE IF NOT EXISTS qcm_choices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    choice_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    explanation TEXT,
    FOREIGN KEY (question_id) REFERENCES qcm_questions(id)
);

CREATE TABLE IF NOT EXISTS qcm_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    qcm_id BIGINT NOT NULL,
    learner_id BIGINT NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    total_questions INT NOT NULL,
    correct_answers INT NOT NULL,
    duration_seconds INT,
    attempted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (qcm_id) REFERENCES qcms(id),
    FOREIGN KEY (learner_id) REFERENCES users(id)
);

-- PROGRESSION APPRENANTS
CREATE TABLE IF NOT EXISTS learner_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    learner_id BIGINT NOT NULL,
    level_id BIGINT NOT NULL,
    courses_completed INT DEFAULT 0,
    sessions_attended INT DEFAULT 0,
    qcm_avg_score DECIMAL(5,2) DEFAULT 0,
    total_minutes INT DEFAULT 0,
    completion_percentage DECIMAL(5,2) DEFAULT 0,
    certified BOOLEAN DEFAULT FALSE,
    certified_at DATETIME,
    UNIQUE KEY uk_learner_level (learner_id, level_id),
    FOREIGN KEY (learner_id) REFERENCES users(id),
    FOREIGN KEY (level_id) REFERENCES levels(id)
);

-- CERTIFICATS
CREATE TABLE IF NOT EXISTS certificates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    learner_id BIGINT NOT NULL,
    level_id BIGINT NOT NULL,
    certificate_number VARCHAR(100) NOT NULL UNIQUE,
    pdf_path TEXT NOT NULL,
    pdf_sha256 VARCHAR(64) NOT NULL,
    signed BOOLEAN DEFAULT TRUE,
    issued_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (learner_id) REFERENCES users(id),
    FOREIGN KEY (level_id) REFERENCES levels(id)
);

-- MESSAGERIE
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    read_at DATETIME,
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (recipient_id) REFERENCES users(id)
);

-- AUDIT LOG
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT,
    action VARCHAR(255) NOT NULL,
    entity VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (actor_id) REFERENCES users(id)
);

-- INDEX PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_courses_level ON courses(level_id);
CREATE INDEX IF NOT EXISTS idx_courses_trainer ON courses(trainer_id);
CREATE INDEX IF NOT EXISTS idx_sessions_level ON streaming_sessions(level_id);
CREATE INDEX IF NOT EXISTS idx_sessions_start ON streaming_sessions(scheduled_start);
CREATE INDEX IF NOT EXISTS idx_progress_learner ON learner_progress(learner_id);
CREATE INDEX IF NOT EXISTS idx_messages_recipient ON messages(recipient_id);

-- ADMIN PAR DEFAUT (mot de passe: Admin@1234 en BCrypt)
INSERT IGNORE INTO users (email, password_hash, first_name, last_name, role, email_verified, active)
VALUES ('admin@sprachreise.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2', 'Admin', 'SprachReise', 'ADMIN', TRUE, TRUE);
