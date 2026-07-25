package user.service.Serivce.Authorization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.service.Dto.AuthResponse;
import user.service.Dto.LoginRequest;
import user.service.Dto.RegisterRequest;
import user.service.Dto.UpdateUSerAfterConnect;
import user.service.Entity.Enum.Compte;
import user.service.Entity.Enum.NiveauEtude;
import user.service.Entity.Enum.Role;
import user.service.Entity.User;
import user.service.Repository.UserRepository;
import user.service.Serivce.FileService;
import user.service.Serivce.UserCommun.UserFinderService;
import user.service.Serivce.WebSocket.AdminRealtimeService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final UserFinderService userFinderService;
    private final FileService fileService;
    private final AdminRealtimeService realtimeService;

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @Transactional
    public User register(RegisterRequest dto) {
        if (dto.getRole() == Role.EMPLOYEE) {
            throw new RuntimeException(
                    "Le rôle EMPLOYEE ne peut pas être choisi à l'inscription.");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email déjà utilisé : " + dto.getEmail());
        }

        String keycloakId = keycloakService.createUserInKeycloak(dto);
        log.info("Keycloak ID obtenu : {}", keycloakId);

        User user = User.builder()
                .keycloakId(keycloakId)
                .email(dto.getEmail())
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .role(dto.getRole())
                .etatCompte(Compte.ACTIF)
                .build();

        User saved = userRepository.save(user);
        log.info("User sauvegardé en DB, ID : {}", saved.getId());
        try {
            realtimeService.notifyNewUser(saved.getPrenom(), saved.getNom(), saved.getRole().name());
        } catch (Exception e) {
            log.warn("WS notify newUser échoué : {}", e.getMessage());
        }
        return saved;
    }

    @Transactional
    public User createBootstrapEmployee(String email, String password, String nom, String prenom) {
        if (this.userFinderService.existsByEmail(email)) {
            throw new IllegalStateException("Le compte bootstrap existe déjà : " + email);
        }

        RegisterRequest fakeDto = RegisterRequest.builder()
                .email(email)
                .password(password)
                .nom(nom)
                .prenom(prenom)
                .role(Role.EMPLOYEE)
                .build();

        String keycloakId = keycloakService.createUserInKeycloak(fakeDto);

        User user = User.builder()
                .keycloakId(keycloakId)
                .email(email)
                .nom(nom)
                .prenom(prenom)
                .role(Role.EMPLOYEE)
                .etatCompte(Compte.ACTIF)
                .build();
        log.info(" create Bootstrap Employee avec success ", user);
        return userRepository.save(user);
    }

    public AuthResponse login(LoginRequest dto) {
        User user = this.userFinderService.byEmail(dto.getEmail());
        if (user.getEtatCompte() == Compte.INACTIF) {
            throw new RuntimeException("Compte désactivé. Contacte l'administrateur.");
        }

        AuthResponse response = keycloakService.login(dto);
        response.setEmail(user.getEmail());
        response.setNom(user.getNom());
        response.setPrenom(user.getPrenom());
        response.setRole(user.getRole().name());
        response.setId(user.getId());
        response.setKeycloakId(user.getKeycloakId());
        log.info(" done login with user  ", response.getEmail(), response.getRole());
        try {
            realtimeService.notifyLoginActivity(user.getId(), user.getPrenom(), user.getNom(), "LOGIN", user.getEmail());
        } catch (Exception e) {
            log.warn("WS notify login échoué : {}", e.getMessage());
        }
        return response;

    }

    @Transactional
    public User syncGoogleUser(String keycloakId, String email, String nom, String prenom) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    log.info("Nouvelle connexion Google (signup) : {}", email);

                    if (this.userFinderService.existsByEmail(email)) {
                        throw new RuntimeException("Email déjà utilisé avec un compte classique !");
                    }

                    User newUser = User.builder()
                            .keycloakId(keycloakId)
                            .email(email)
                            .nom(nom != null ? nom : "")
                            .prenom(prenom != null ? prenom : "")
                            .etatCompte(Compte.ACTIF)
                            .build();

                    return userRepository.save(newUser);
                });
    }


    @Transactional
    public User completeProfile(String keycloakId, UpdateUSerAfterConnect dto) {
        User user = this.userFinderService.getUserByKeycloakId(keycloakId);

        if (dto.getRole() != null) {
            if (dto.getRole() == Role.EMPLOYEE) {
                throw new RuntimeException(
                        "Le rôle EMPLOYEE ne peut pas être assigné via cette route.");
            }
            if (user.getRole() != null && user.getRole() != dto.getRole()) {
                throw new RuntimeException("Le rôle est déjà défini et ne peut pas être modifié.");
            }
            if (user.getRole() == null) {
                user.setRole(dto.getRole());
                keycloakService.assignRoleInKeycloak(keycloakId, dto.getRole().name());
            }
        }

        Role effectiveRole = user.getRole();
        if (effectiveRole == null) {
            throw new RuntimeException("Le rôle doit être choisi avant de compléter le profil.");
        }

        validateRoleSpecificFields(effectiveRole, dto);

        if (dto.getNom() != null) user.setNom(dto.getNom());
        if (dto.getPrenom() != null) user.setPrenom(dto.getPrenom());
        if (dto.getAdresse() != null) user.setAdresse(dto.getAdresse());
        if (dto.getDescription() != null) user.setDescription(dto.getDescription());
        if (dto.getGenre() != null) user.setGenre(dto.getGenre());
        if (dto.getNum_Tel() != null) user.setNum_Tel(dto.getNum_Tel());
        if (dto.getDateNaissance() != null) user.setDateNaissance(dto.getDateNaissance());
        if (dto.getLinkedin() != null) user.setLinkedin(dto.getLinkedin());
        if (dto.getTwitter() != null) user.setTwitter(dto.getTwitter());
        if (dto.getSiteweb() != null) user.setSiteweb(dto.getSiteweb());

        if (dto.getSpecialiteEtude() != null) user.setSpecialiteEtude(dto.getSpecialiteEtude());
        if (dto.getUniversiteEtude() != null) user.setUniversiteEtude(dto.getUniversiteEtude());
        if (dto.getAnneesExperience() != null) user.setAnneesExperience(dto.getAnneesExperience());
        if (dto.getNiveauEtude() != null && !dto.getNiveauEtude().isBlank()) {
            try {
                user.setNiveauEtude(NiveauEtude.valueOf(dto.getNiveauEtude()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Niveau d'étude invalide : " + dto.getNiveauEtude());
            }
        }

        // --- 4. Email ---
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userFinderService.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("Cet email est déjà utilisé");
            }
            user.setEmail(dto.getEmail());
        }
        keycloakService.updateUserInKeycloak(keycloakId, dto);

        if (dto.getImageBase64() != null && !dto.getImageBase64().isBlank()) {
            byte[] decoded = fileService.decodeBase64(dto.getImageBase64());
            if (decoded == null) {
                throw new RuntimeException("Image Base64 invalide");
            }
            if (!fileService.isValidFileSize(decoded, 2L * 1024 * 1024)) {
                throw new RuntimeException("Image trop volumineuse (max 2MB)");
            }
            user.setImage(decoded);
        }

        User saved = userRepository.save(user);
        log.info("Profil complété pour {} (rôle={})", keycloakId, effectiveRole);
        return saved;
    }

    private void validateRoleSpecificFields(Role role, UpdateUSerAfterConnect dto) {
        if (role == Role.CANDIDAT || role == Role.EMPLOYEE) {
            boolean missingSpecialite = isBlank(dto.getSpecialiteEtude());
            boolean missingNiveau = dto.getNiveauEtude() == null || dto.getNiveauEtude().isBlank();
            if (missingSpecialite || missingNiveau) {
                throw new RuntimeException(
                        "La spécialité et le niveau d'étude sont requis pour votre rôle.");
            }
        }
    }

}