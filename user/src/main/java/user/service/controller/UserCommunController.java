package user.service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import user.service.Dto.UpdateUSerAfterConnect;
import user.service.Entity.Enum.Role;
import user.service.Entity.User;
import user.service.Serivce.UserService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CANDIDAT','RH','EMPLOYEE')")
public class UserCommunController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getUserByKeycloakId(jwt.getSubject());

        String imageBase64 = null;
        if (user.getImage() != null && user.getImage().length > 0) {
            imageBase64 = "data:image/jpeg;base64,"
                    + java.util.Base64.getEncoder().encodeToString(user.getImage());
        }

        return ResponseEntity.ok(Map.ofEntries(
                Map.entry("id", user.getId()),
                Map.entry("keycloakId", user.getKeycloakId()),
                Map.entry("email", user.getEmail()),
                Map.entry("nom", user.getNom()),
                Map.entry("prenom", user.getPrenom()),
                Map.entry("role", user.getRole() != null ? user.getRole().name() : ""),
                Map.entry("genre", user.getGenre() != null ? user.getGenre().name() : ""),
                Map.entry("adresse", user.getAdresse() != null ? user.getAdresse() : ""),
                Map.entry("description", user.getDescription() != null ? user.getDescription() : ""),
                Map.entry("num_Tel", user.getNum_Tel() != null ? user.getNum_Tel() : 0),
                Map.entry("dateNaissance", user.getDateNaissance() != null ? user.getDateNaissance().toString() : ""),
                Map.entry("specialiteEtude", user.getSpecialiteEtude() != null ? user.getSpecialiteEtude() : ""),
                Map.entry("niveauEtude", user.getNiveauEtude() != null ? user.getNiveauEtude().name() : ""),
                Map.entry("anneesExperience", user.getAnneesExperience() != null ? user.getAnneesExperience() : 0),
                Map.entry("linkedin", user.getLinkedin() != null ? user.getLinkedin() : ""),
                Map.entry("twitter", user.getTwitter() != null ? user.getTwitter() : ""),
                Map.entry("siteweb", user.getSiteweb() != null ? user.getSiteweb() : ""),
                Map.entry("etatCompte", user.getEtatCompte().name()),
                Map.entry("imageBase64", imageBase64 != null ? imageBase64 : "")
        ));
    }

    @PutMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@AuthenticationPrincipal Jwt jwt,
                                             @RequestBody UpdateUSerAfterConnect dto) {
        try {
            String keycloakId = jwt.getSubject();
            User existing = userService.getUserByKeycloakId(keycloakId);

            String validationError = validateRoleSpecificFields(existing.getRole(), dto);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", validationError));
            }

            User user = userService.completeProfile(keycloakId, dto);
            return ResponseEntity.ok(Map.of(
                    "message", "Profil complété",
                    "role", user.getRole() != null ? user.getRole().name() : "NON_DEFINI"
            ));
        } catch (RuntimeException e) {
            log.error("Erreur complete-profile : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Certains champs n'ont de sens que pour certains rôles :
     * - EMPLOYEE / CANDIDAT : parcours d'études attendu (spécialité, niveau).
     * - RH : ces champs restent optionnels, non pertinents pour son activité.
     * Retourne un message d'erreur si la règle n'est pas respectée, null sinon.
     */
    private String validateRoleSpecificFields(Role role, UpdateUSerAfterConnect dto) {
        if (role == null) {
            return null; // rôle pas encore assigné (ex: premier passage Google) : pas de règle à appliquer
        }

        if (role == Role.EMPLOYEE || role == Role.CANDIDAT) {
            boolean missingSpecialite = dto.getSpecialiteEtude() == null || dto.getSpecialiteEtude().isBlank();
            boolean missingNiveau = dto.getNiveauEtude() == null || dto.getNiveauEtude().isBlank();

            if (missingSpecialite || missingNiveau) {
                return "La spécialité et le niveau d'étude sont requis pour votre rôle.";
            }
        }

        return null;
    }
}