package com.sprachreise.api.config;

import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.TrainerProfile;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.TrainerProfileRepository;
import com.sprachreise.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DIPLOMA_PATH = "diplomas/diploma_1780412471953.pdf";
    private static final String TRAINER_PWD  = "Formateur@2026";
    private static final String LEARNER_PWD  = "Apprenant@2026";

    private final UserRepository           userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final PasswordEncoder          passwordEncoder;

    public DataInitializer(UserRepository u, TrainerProfileRepository t, PasswordEncoder p) {
        this.userRepository           = u;
        this.trainerProfileRepository = t;
        this.passwordEncoder          = p;
    }

    private static final String OLD_BROKEN_HASH = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4VBjSHEkK2";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createAdmin();
        createTrainers();
        createLearners();
        fixBrokenPasswords();
        log.info("=== DataInitializer : initialisation terminée ===");
    }

    /** Corrige tous les comptes qui ont l'ancien hash BCrypt hardcodé non fonctionnel */
    private void fixBrokenPasswords() {
        List<User> broken = userRepository.findAll().stream()
            .filter(u -> OLD_BROKEN_HASH.equals(u.getPasswordHash()))
            .toList();
        for (User u : broken) {
            String newPwd = u.getRole() == Role.TRAINER ? TRAINER_PWD : LEARNER_PWD;
            u.setPasswordHash(passwordEncoder.encode(newPwd));
            userRepository.save(u);
            log.info("Mot de passe corrigé pour : {} ({})", u.getEmail(), u.getRole());
        }
        if (!broken.isEmpty()) log.info("{} compte(s) avec hash cassé réinitialisé(s)", broken.size());
    }

    // ── Admin ─────────────────────────────────────────────────────────────────
    private void createAdmin() {
        ensureUser("admin@sprachreise.com", "Admin@1234", Role.ADMIN, "Admin", "SprachReise", null, null);
    }

    // ── Formateurs certifiés (2 par niveau CECRL) ─────────────────────────────
    private void createTrainers() {
        Object[][] trainers = {
            // email, prénom, nom, niveau, langue maternelle, bio
            {"schmidt.anna@sprachreise.com",   "Anna",    "Schmidt",   "A1", "Allemand",
             "Diplômée du Goethe-Institut, 8 ans d'expérience avec les débutants."},
            {"mueller.karl@sprachreise.com",   "Karl",    "Müller",    "A1", "Allemand",
             "Professeur certifié telc, spécialiste de la phonétique allemande."},
            {"weber.lisa@sprachreise.com",     "Lisa",    "Weber",     "A2", "Allemand",
             "Master en linguistique germanique, ancienne professeure au Goethe-Institut de Yaoundé."},
            {"braun.thomas@sprachreise.com",   "Thomas",  "Braun",     "A2", "Allemand",
             "Certifié DSH, passionné par l'apprentissage interculturel franco-allemand."},
            {"hoffmann.marie@sprachreise.com", "Marie",   "Hoffmann",  "B1", "Français",
             "Bilingue franco-allemande, titulaire du TestDaF niveau 4, 10 ans d'enseignement."},
            {"klein.peter@sprachreise.com",    "Peter",   "Klein",     "B1", "Allemand",
             "Formateur certifié Goethe-Zertifikat B1, spécialisé en préparation aux examens officiels."},
            {"becker.sarah@sprachreise.com",   "Sarah",   "Becker",    "B2", "Allemand",
             "Doctorante en études germaniques, experte en grammaire avancée et compréhension écrite."},
            {"richter.hans@sprachreise.com",   "Hans",    "Richter",   "B2", "Allemand",
             "Certifié telc B2, 15 ans d'expérience dans la formation professionnelle en allemand."},
            {"wolf.ingrid@sprachreise.com",    "Ingrid",  "Wolf",      "C1", "Allemand",
             "Professeure universitaire, titulaire du DSH 3, spécialiste de la littérature allemande."},
            {"neumann.jochen@sprachreise.com", "Jochen",  "Neumann",   "C1", "Allemand",
             "Expert en allemand des affaires, certifié Goethe-Zertifikat C1, ancien interprète."},
            {"zimmermann.eva@sprachreise.com", "Eva",     "Zimmermann","C2", "Allemand",
             "Traductrice assermentée et professeure certifiée C2, ancienne rédactrice pour ZDF."},
            {"hartmann.felix@sprachreise.com", "Felix",   "Hartmann",  "C2", "Allemand",
             "Docteur en philologie germanique, formateur certifié pour le niveau C2 CECRL."},
        };

        for (Object[] t : trainers) {
            String email = (String) t[0];
            if (userRepository.existsByEmail(email)) continue;

            User u = new User();
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(TRAINER_PWD));
            u.setFirstName((String) t[1]);
            u.setLastName((String) t[2]);
            u.setRole(Role.TRAINER);
            u.setActive(true);
            u.setLevelCode((String) t[3]);
            u.setPhone("+237699" + (100000 + (int)(Math.random() * 899999)));
            u.setCity("Yaoundé");
            u.setBio((String) t[5]);
            userRepository.save(u);

            TrainerProfile p = new TrainerProfile();
            p.setUserId(u.getId());
            p.setTeachingLevelCode((String) t[3]);
            p.setTeachingLanguageCode("de");
            p.setNativeLanguage((String) t[4]);
            p.setMaxStudents(20);
            p.setCurrentStudents(0);
            p.setStatus(TrainerProfile.Status.APPROVED);
            p.setDiplomaPdfPath(DIPLOMA_PATH);
            p.setMotivation("Formateur certifié ayant rejoint SprachReise pour partager sa passion de l'allemand.");
            p.setReviewedAt(LocalDateTime.now());
            trainerProfileRepository.save(p);

            log.info("Formateur créé : {} {} ({})", t[1], t[2], t[3]);
        }
    }

    // ── Apprenants (5 par niveau) ─────────────────────────────────────────────
    private void createLearners() {
        Object[][] learners = {
            // email, prénom, nom, niveau, ville
            {"alice.fouda@gmail.com",   "Alice",   "Fouda",   "A1", "Yaoundé"},
            {"boris.mbarga@gmail.com",  "Boris",   "Mbarga",  "A1", "Douala"},
            {"claire.ngo@gmail.com",    "Claire",  "Ngo",     "A1", "Bafoussam"},
            {"david.eto@gmail.com",     "David",   "Eto",     "A1", "Garoua"},
            {"emilie.tam@gmail.com",    "Emilie",  "Tam",     "A1", "Bertoua"},
            {"frank.awono@gmail.com",   "Frank",   "Awono",   "A2", "Yaoundé"},
            {"grace.biya@gmail.com",    "Grace",   "Biya",    "A2", "Douala"},
            {"henry.nguema@gmail.com",  "Henry",   "Nguema",  "A2", "Ngaoundéré"},
            {"irene.mvondo@gmail.com",  "Irène",   "Mvondo",  "A2", "Kribi"},
            {"jules.belinga@gmail.com", "Jules",   "Belinga",  "A2", "Ebolowa"},
            {"karine.ateba@gmail.com",  "Karine",  "Ateba",   "B1", "Yaoundé"},
            {"leo.menye@gmail.com",     "Léo",     "Menyé",   "B1", "Douala"},
            {"marie.obam@gmail.com",    "Marie",   "Obam",    "B1", "Mbalmayo"},
            {"noel.tsimi@gmail.com",    "Noël",    "Tsimi",   "B1", "Edéa"},
            {"odile.nkomo@gmail.com",   "Odile",   "Nkomo",   "B1", "Yaoundé"},
            {"pascal.eba@gmail.com",    "Pascal",  "Eba",     "B2", "Douala"},
            {"queen.ekindi@gmail.com",  "Queen",   "Ekindi",  "B2", "Yaoundé"},
            {"remy.zang@gmail.com",     "Rémy",    "Zang",    "B2", "Bamenda"},
            {"stella.ondo@gmail.com",   "Stella",  "Ondo",    "B2", "Limbe"},
            {"theo.abega@gmail.com",    "Théo",    "Abega",   "B2", "Yaoundé"},
            {"ursula.ndo@gmail.com",    "Ursula",  "Ndo",     "C1", "Douala"},
            {"victor.messa@gmail.com",  "Victor",  "Messa",   "C1", "Yaoundé"},
            {"wanda.fono@gmail.com",    "Wanda",   "Fono",    "C1", "Bafoussam"},
            {"xavier.manga@gmail.com",  "Xavier",  "Manga",   "C1", "Ngaoundéré"},
            {"yves.mvoe@gmail.com",     "Yves",    "Mvoe",    "C1", "Douala"},
            {"zara.bello@gmail.com",    "Zara",    "Bello",   "C2", "Yaoundé"},
            {"adam.onana@gmail.com",    "Adam",    "Onana",   "C2", "Douala"},
            {"brice.foumena@gmail.com", "Brice",   "Foumena", "C2", "Buea"},
            {"celia.njoya@gmail.com",   "Célia",   "Njoya",   "C2", "Yaoundé"},
            {"dany.makongo@gmail.com",  "Dany",    "Makongo", "C2", "Douala"},
        };

        for (Object[] l : learners) {
            String email = (String) l[0];
            if (userRepository.existsByEmail(email)) continue;

            String levelCode = (String) l[3];
            User u = new User();
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(LEARNER_PWD));
            u.setFirstName((String) l[1]);
            u.setLastName((String) l[2]);
            u.setRole(Role.LEARNER);
            u.setActive(true);
            u.setLevelCode(levelCode);
            u.setCity((String) l[4]);
            u.setPhone("+237655" + (100000 + (int)(Math.random() * 899999)));

            // Assigner un formateur disponible
            List<TrainerProfile> available = trainerProfileRepository.findAvailableByLevel(levelCode);
            if (!available.isEmpty()) {
                TrainerProfile trainer = available.get(0);
                u.setAssignedTrainerId(trainer.getUserId());
                trainer.setCurrentStudents(trainer.getCurrentStudents() + 1);
                trainerProfileRepository.save(trainer);
            }
            userRepository.save(u);
            log.info("Apprenant créé : {} {} ({}) → formateur {}", l[1], l[2], levelCode, u.getAssignedTrainerId());
        }
    }

    private void ensureUser(String email, String rawPwd, Role role,
                            String firstName, String lastName,
                            String levelCode, String city) {
        if (!userRepository.existsByEmail(email)) {
            User u = new User();
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(rawPwd));
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setRole(role);
            u.setActive(true);
            if (levelCode != null) u.setLevelCode(levelCode);
            if (city != null) u.setCity(city);
            userRepository.save(u);
            log.info("Compte {} créé : {}", role, email);
        }
    }
}
