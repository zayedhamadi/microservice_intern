package user.service.Serivce;


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
import user.service.Entity.User;
import user.service.Repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;

    @Transactional
    public User register(RegisterRequest dto) {
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
                .genre(dto.getGenre())
                .adresse(dto.getAdresse())
                .description(dto.getDescription())
                .dateNaissance(dto.getDateNaissance())
                .num_Tel(dto.getNum_Tel())
                .etatCompte(Compte.ACTIF)
                .build();

        User saved = userRepository.save(user);
        log.info("User sauvegardé en DB, ID : {}", saved.getId());
        return saved;
    }

    public AuthResponse login(LoginRequest dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Aucun compte trouvé pour : " + dto.getEmail()));

        if (user.getEtatCompte() == Compte.INACTIF) {
            throw new RuntimeException("Compte désactivé. Contactez l'administrateur.");
        }

        AuthResponse response = keycloakService.login(dto);
        response.setEmail(user.getEmail());
        response.setNom(user.getNom());
        response.setPrenom(user.getPrenom());
        response.setRole(user.getRole().name());
        response.setId(user.getId());
        response.setKeycloakId(user.getKeycloakId());
        return response;
    }

    /**
     * Appelé après connexion Google. Si l'utilisateur n'existe pas encore en DB,
     * il est créé avec le strict minimum (email, nom, prénom). Le reste du profil
     * (rôle, adresse, etc.) sera complété via completeProfile().
     */
    @Transactional
    public User syncGoogleUser(String keycloakId, String email, String nom, String prenom) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    log.info("Nouvelle connexion Google (signup) : {}", email);

                    if (userRepository.existsByEmail(email)) {
                        throw new RuntimeException("Email déjà utilisé avec un compte classique !");
                    }

                    User newUser = User.builder()
                            .keycloakId(keycloakId)
                            .email(email)
                            .nom(nom != null ? nom : "")
                            .prenom(prenom != null ? prenom : "")
                            .etatCompte(Compte.ACTIF)
                            .build();
                    // Pas de rôle assigné ici : forcé de compléter le profil (voir completeProfile)

                    return userRepository.save(newUser);
                });
    }

    /**
     * Complète le profil après signup (Google ou classique) : rôle, coordonnées, etc.
     */
    @Transactional
    public User completeProfile(String keycloakId, UpdateUSerAfterConnect dto) {
        User user = getUserByKeycloakId(keycloakId);

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
        if (dto.getNiveauEtude() != null) user.setNiveauEtude(NiveauEtude.valueOf(dto.getNiveauEtude()));
        if (dto.getAnneesExperience() != null) user.setAnneesExperience(dto.getAnneesExperience());

        // Rôle assigné une seule fois (à la complétion du profil, si pas déjà défini)
        if (dto.getRole() != null && user.getRole() == null) {
            user.setRole(dto.getRole());
            keycloakService.assignRoleInKeycloak(keycloakId, dto.getRole().name());
        }

        // Email modifiable seulement si différent et libre
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("Cet email est déjà utilisé");
            }
            user.setEmail(dto.getEmail());
        }
        keycloakService.updateUserInKeycloak(keycloakId, dto);

        if (dto.getImageBase64() != null && !dto.getImageBase64().isBlank()) {
            try {
                String base64 = dto.getImageBase64();
                if (base64.contains(",")) base64 = base64.substring(base64.indexOf(",") + 1);
                user.setImage(java.util.Base64.getDecoder().decode(base64));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Image Base64 invalide : " + e.getMessage());
            }
        }

        User saved = userRepository.save(user);
        log.info("Profil complété pour {}", keycloakId);
        return saved;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User non trouvé avec ID : " + id));
    }

    public User getUserByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User non trouvé avec keycloakId : " + keycloakId));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        keycloakService.deleteUserFromKeycloak(user.getKeycloakId());
        userRepository.delete(user);
        log.info("User {} supprimé", id);
    }

    @Transactional
    public void changeUserStatus(Long id, boolean activate) {
        User user = getUserById(id);
        user.setEtatCompte(activate ? Compte.ACTIF : Compte.INACTIF);
        userRepository.save(user);
        log.info("User {} → {}", id, activate ? "ACTIF" : "INACTIF");
    }
}