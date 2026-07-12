package user.service.Serivce;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.service.Dto.UpdateUserRequest;
import user.service.Entity.Enum.Compte;
import user.service.Entity.User;
import user.service.Repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UserCommunService {

    UserRepository userRepository;
    UserFinderService userFinderService;
    KeycloakService keycloakService; // gère le changement de mdp côté Keycloak

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void changeUserStatus(Long id, boolean activate) {
        User user = this.userFinderService.byId(id);
        user.setEtatCompte(activate ? Compte.ACTIF : Compte.INACTIF);
        userRepository.save(user);
        log.info("User {} → {}", id, activate ? "ACTIF" : "INACTIF");
    }

    @Transactional
    public User getUserByKeycloakId(String id) {
        return this.userFinderService.getUserByKeycloakId(id);
    }

    @Transactional
    public User updateUser(String keycloakId, UpdateUserRequest request) {
        User user = this.userFinderService.getUserByKeycloakId(keycloakId);

        // --- Infos communes à tous les rôles ---
        if (request.getNom() != null) user.setNom(request.getNom());
        if (request.getPrenom() != null) user.setPrenom(request.getPrenom());
        if (request.getAdresse() != null) user.setAdresse(request.getAdresse());
        if (request.getDescription() != null) user.setDescription(request.getDescription());
        if (request.getDateNaissance() != null) user.setDateNaissance(request.getDateNaissance());
        if (request.getNum_Tel() != null) user.setNum_Tel(request.getNum_Tel());
        if (request.getGenre() != null) user.setGenre(request.getGenre());
        if (request.getAnneesExperience() != null) user.setAnneesExperience(request.getAnneesExperience());
        if (request.getLinkedin() != null) user.setLinkedin(request.getLinkedin());
        if (request.getTwitter() != null) user.setTwitter(request.getTwitter());
        if (request.getSiteweb() != null) user.setSiteweb(request.getSiteweb());

        // --- CV : disponible pour TOUS les rôles, indépendant du cursus académique ---
        if (request.getCvBase64() != null && !request.getCvBase64().isEmpty()) {
            try {
                String base64 = request.getCvBase64();
                if (base64.contains(",")) base64 = base64.split(",")[1];
                user.setCvUser(java.util.Base64.getDecoder().decode(base64));
            } catch (Exception e) {
                log.error("Erreur décodage CV : {}", e.getMessage());
                throw new RuntimeException("CV invalide");
            }
        }

        // --- Champs réservés aux rôles nécessitant un cursus académique (CANDIDAT / EMPLOYEE) ---
        if (user.requiresEtudes()) {
            if (request.getSpecialiteEtude() != null) user.setSpecialiteEtude(request.getSpecialiteEtude());
            if (request.getUniversiteEtude() != null) user.setUniversiteEtude(request.getUniversiteEtude());
            if (request.getNiveauEtude() != null) user.setNiveauEtude(request.getNiveauEtude());
        } else if (request.getSpecialiteEtude() != null || request.getNiveauEtude() != null) {
            log.warn("Champs d'études ignorés pour {} (rôle {} ne nécessite pas d'études)", keycloakId, user.getRole());
        }

        // --- Image ---
        if (request.getImageBase64() != null && !request.getImageBase64().isEmpty()) {
            try {
                String base64 = request.getImageBase64();
                if (base64.contains(",")) base64 = base64.split(",")[1];
                user.setImage(java.util.Base64.getDecoder().decode(base64));
            } catch (Exception e) {
                log.error("Erreur décodage image : {}", e.getMessage());
                throw new RuntimeException("Image invalide");
            }
        }

        // --- Mot de passe (optionnel) ---
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank())
                throw new RuntimeException("Le mot de passe actuel est requis.");
            if (!request.getNewPassword().equals(request.getConfirmPassword()))
                throw new RuntimeException("Les mots de passe ne correspondent pas.");
            if (request.getNewPassword().length() < 8)
                throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 8 caractères.");

            keycloakService.verifyCurrentPassword(user.getEmail(), request.getCurrentPassword());
            keycloakService.changePassword(keycloakId, request.getNewPassword());
        }

        User updated = userRepository.save(user);
        log.info("✅ User {} ({}) mis à jour", keycloakId, user.getRole());
        return updated;
    }
}