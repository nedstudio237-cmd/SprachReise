package com.sprachreise.api.controller;

import com.sprachreise.api.dto.AuthResponse;
import com.sprachreise.api.dto.LoginRequest;
import com.sprachreise.api.dto.RegisterRequest;
import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.TrainerProfile;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.TrainerProfileRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.security.JwtUtil;
import com.sprachreise.api.service.LoggingMailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Set<String> VALID_LEVELS = Set.of("A1","A2","B1","B2","C1","C2");
    private static final Set<String> VALID_LANGS  = Set.of("de","en","es","zh");

    @org.springframework.beans.factory.annotation.Value("${storage.upload-dir}")
    private String storageDir;

    private final UserRepository         userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final PasswordEncoder         passwordEncoder;
    private final JwtUtil                 jwtUtil;
    private final AuthenticationManager   authenticationManager;
    private final LoggingMailService      mailService;

    public AuthController(UserRepository userRepository,
                          TrainerProfileRepository trainerProfileRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          AuthenticationManager authenticationManager,
                          LoggingMailService mailService) {
        this.userRepository          = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.passwordEncoder         = passwordEncoder;
        this.jwtUtil                 = jwtUtil;
        this.authenticationManager   = authenticationManager;
        this.mailService             = mailService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Cet email est déjà utilisé"));
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(Role.LEARNER);

        // Niveau choisi lors de l'onboarding (peut être null → A1 par défaut)
        String levelCode = (request.getLevelCode() != null && !request.getLevelCode().isBlank())
                ? request.getLevelCode().toUpperCase() : "A1";
        user.setLevelCode(levelCode);

        // Auto-assignation au formateur disponible ayant le moins d'élèves
        List<TrainerProfile> available = trainerProfileRepository.findAvailableByLevel(levelCode);
        if (!available.isEmpty()) {
            TrainerProfile trainer = available.get(0);
            user.setAssignedTrainerId(trainer.getUserId());
            trainer.setCurrentStudents(trainer.getCurrentStudents() + 1);
            trainerProfileRepository.save(trainer);
        }

        userRepository.save(user);

        String accessToken  = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new AuthResponse(accessToken, refreshToken,
                        user.getId(), user.getEmail(),
                        user.getFirstName(), user.getLastName(),
                        user.getRole().name(), user.getPhotoUrl()));
    }

    // ── Mise à jour niveau après onboarding ──────────────────────────────────
    @PostMapping("/set-level")
    public ResponseEntity<?> setLevel(@RequestBody Map<String, String> body,
                                      @RequestHeader("Authorization") String authHeader) {
        String token = authHeader != null ? authHeader.replace("Bearer ", "") : null;
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String email;
        try { email = jwtUtil.extractEmail(token); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        String levelCode = body.getOrDefault("levelCode", "A1").toUpperCase().trim();
        if (levelCode.isBlank()) levelCode = "A1";

        // Libérer l'ancien formateur si déjà assigné
        if (user.getAssignedTrainerId() != null) {
            trainerProfileRepository.findByUserId(user.getAssignedTrainerId()).ifPresent(old -> {
                if (old.getCurrentStudents() > 0) old.setCurrentStudents(old.getCurrentStudents() - 1);
                trainerProfileRepository.save(old);
            });
        }

        // Assigner le formateur du nouveau niveau ayant le moins d'élèves
        user.setLevelCode(levelCode);
        List<TrainerProfile> available = trainerProfileRepository.findAvailableByLevel(levelCode);
        Long assignedTrainerId = null;
        if (!available.isEmpty()) {
            TrainerProfile trainer = available.get(0);
            assignedTrainerId = trainer.getUserId();
            user.setAssignedTrainerId(assignedTrainerId);
            trainer.setCurrentStudents(trainer.getCurrentStudents() + 1);
            trainerProfileRepository.save(trainer);
        } else {
            user.setAssignedTrainerId(null);
        }
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "levelCode", levelCode,
            "assignedTrainerId", assignedTrainerId != null ? assignedTrainerId : 0
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email ou mot de passe incorrect"));
        }
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String accessToken  = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        String trainerStatus = null;
        if (user.getRole().name().equals("TRAINER")) {
            trainerStatus = trainerProfileRepository.findByUserId(user.getId())
                    .map(p -> p.getStatus().name())
                    .orElse("PENDING");
        }

        Long assignedTrainerId = user.getRole() == Role.LEARNER ? user.getAssignedTrainerId() : null;

        return ResponseEntity.ok(
                new AuthResponse(accessToken, refreshToken,
                        user.getId(), user.getEmail(),
                        user.getFirstName(), user.getLastName(),
                        user.getRole().name(), user.getPhotoUrl(), trainerStatus, assignedTrainerId));
    }

    // ── Inscription formateur ─────────────────────────────────────────────────

    @PostMapping("/trainer-register")
    public ResponseEntity<?> trainerRegister(@RequestBody Map<String, String> body) {
        String firstName      = trim(body.get("firstName"));
        String lastName       = trim(body.get("lastName"));
        String email          = trim(body.get("email")).toLowerCase();
        String password       = trim(body.get("password"));
        String phone          = trim(body.get("phone"));
        String bio            = trim(body.get("bio"));
        String nativeLanguage = trim(body.get("nativeLanguage"));
        String levelCode      = trim(body.get("teachingLevelCode")).toUpperCase();
        String langCode       = trim(body.get("teachingLanguageCode"));
        String motivation     = trim(body.get("motivation"));
        String diplomaBase64  = trim(body.get("diplomaBase64"));
        String diplomaName    = trim(body.get("diplomaFileName"));

        // Validations
        if (firstName.isEmpty() || lastName.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Prénom et nom requis"));
        if (email.isEmpty() || !email.contains("@"))
            return ResponseEntity.badRequest().body(Map.of("error", "Email invalide"));
        if (password.length() < 8)
            return ResponseEntity.badRequest().body(Map.of("error", "Mot de passe : 8 caractères minimum"));
        if (!password.matches(".*[A-Z].*"))
            return ResponseEntity.badRequest().body(Map.of("error", "Mot de passe : au moins 1 majuscule requise"));
        if (!password.matches(".*[0-9].*"))
            return ResponseEntity.badRequest().body(Map.of("error", "Mot de passe : au moins 1 chiffre requis"));
        if (!password.matches(".*[^a-zA-Z0-9].*"))
            return ResponseEntity.badRequest().body(Map.of("error", "Mot de passe : au moins 1 caractère spécial requis"));
        if (!VALID_LEVELS.contains(levelCode))
            return ResponseEntity.badRequest().body(Map.of("error", "Niveau invalide (A1–C2)"));
        if (langCode.isEmpty() || !VALID_LANGS.contains(langCode))
            return ResponseEntity.badRequest().body(Map.of("error", "Langue d'enseignement invalide"));
        if (diplomaBase64.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Le diplôme est obligatoire"));
        if (userRepository.existsByEmail(email))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Cet email est déjà utilisé"));

        // Décoder et sauvegarder le diplôme
        byte[] pdfBytes;
        try {
            pdfBytes = java.util.Base64.getDecoder().decode(diplomaBase64);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier diplôme invalide"));
        }
        if (pdfBytes.length > 5 * 1024 * 1024)
            return ResponseEntity.badRequest().body(Map.of("error", "Diplôme trop volumineux (max 5 Mo)"));

        String diplomaPath = null;
        try {
            String filename = "diploma_" + System.currentTimeMillis() + ".pdf";
            Path dest = Paths.get(storageDir, "diplomas", filename);
            Files.createDirectories(dest.getParent());
            Files.write(dest, pdfBytes);
            diplomaPath = "diplomas/" + filename;
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur sauvegarde du diplôme"));
        }

        // Créer le compte (actif dès l'inscription, accès contrôlé par TrainerProfile.status)
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setBio(bio.isEmpty() ? null : bio);
        user.setRole(Role.TRAINER);
        user.setActive(true);
        userRepository.save(user);

        // Créer le profil formateur
        TrainerProfile profile = new TrainerProfile();
        profile.setUserId(user.getId());
        profile.setNativeLanguage(nativeLanguage.isEmpty() ? null : nativeLanguage);
        profile.setTeachingLevelCode(levelCode);
        profile.setTeachingLanguageCode(langCode);
        profile.setMotivation(motivation.isEmpty() ? null : motivation);
        profile.setDiplomaPdfPath(diplomaPath);
        profile.setStatus(TrainerProfile.Status.PENDING);
        trainerProfileRepository.save(profile);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Candidature soumise avec succès. Vous recevrez un email dès que votre compte est approuvé.",
            "status", "PENDING",
            "profileId", profile.getId()
        ));
    }

    // ── Mot de passe oublié ───────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email requis"));
        }
        email = email.trim().toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Aucun compte trouvé pour cet email"));
        }

        // Génère un mot de passe temporaire : 1 majuscule + 6 chiffres + 1 spécial
        String tempPassword = "Sr" + (100000 + (int)(Math.random() * 900000)) + "!";
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        mailService.sendPasswordReset(email, user.getFirstName(), tempPassword);

        return ResponseEntity.ok(Map.of(
            "message", "Un nouveau mot de passe a été envoyé à " + email
        ));
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
}
