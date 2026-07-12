package user.service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import user.service.Dto.UpdateUSerAfterConnect;
import user.service.Dto.UpdateUserRequest;
import user.service.Entity.User;
import user.service.Serivce.UserCommun.UserCommunService;
import user.service.Serivce.UserCommun.UserFinderService;
import user.service.Serivce.Authorization.UserService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CANDIDAT','RH','EMPLOYEE')")
public class UserCommunController {

    private final UserService userService;
    private final UserCommunService userCommunService;
    private final UserFinderService userFinderService;

    @PutMapping("/updateMyProfile")
    public ResponseEntity<?> updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                             @RequestBody UpdateUserRequest request) {
        try {
            User user = userCommunService.updateUser(jwt.getSubject(), request);
            return ResponseEntity.ok(Map.of(
                    "message", "Profil mis à jour avec succès",
                    "profileComplete", user.isProfileComplete()
            ));
        } catch (RuntimeException e) {
            log.error("Erreur update-profile : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/me/full-profile")
    public ResponseEntity<?> getMyFullProfile(@AuthenticationPrincipal Jwt jwt) {
        // TODO: brancher les vrais services Poste / Département / Responsable / PositionHistory
        // quand ils existeront. Ce stub évite le 500 (NoResourceFoundException) en attendant.
        User user = this.userFinderService.getUserByKeycloakId(jwt.getSubject());
        return ResponseEntity.ok(Map.of(
                "departementActuel", Map.of(),
                "posteActuel", Map.of(),
                "responsable", Map.of(),
                "positionHistory", List.of()
        ));
    }
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        User user = this.userFinderService.getUserByKeycloakId(jwt.getSubject());

        String imageBase64 = null;
        if (user.getImage() != null && user.getImage().length > 0) {
            imageBase64 = "data:image/jpeg;base64,"
                    + java.util.Base64.getEncoder().encodeToString(user.getImage());
        }

        String cvBase64 = null;
        if (user.getCvUser() != null && user.getCvUser().length > 0) {
            cvBase64 = "data:application/pdf;base64,"
                    + java.util.Base64.getEncoder().encodeToString(user.getCvUser());
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
                Map.entry("universiteEtude", user.getUniversiteEtude() != null ? user.getUniversiteEtude() : ""),
                Map.entry("niveauEtude", user.getNiveauEtude() != null ? user.getNiveauEtude().name() : ""),
                Map.entry("anneesExperience", user.getAnneesExperience() != null ? user.getAnneesExperience() : 0),
                Map.entry("linkedin", user.getLinkedin() != null ? user.getLinkedin() : ""),
                Map.entry("twitter", user.getTwitter() != null ? user.getTwitter() : ""),
                Map.entry("siteweb", user.getSiteweb() != null ? user.getSiteweb() : ""),
                Map.entry("etatCompte", user.getEtatCompte().name()),
                Map.entry("imageBase64", imageBase64 != null ? imageBase64 : ""),
                Map.entry("cvBase64", cvBase64 != null ? cvBase64 : ""),
                Map.entry("profileComplete", user.isProfileComplete()),
                Map.entry("missingFields", user.getMissingFields()),
                Map.entry("requiresEtudes", user.requiresEtudes())
        ));
    }

    @PutMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@AuthenticationPrincipal Jwt jwt,
                                             @RequestBody UpdateUSerAfterConnect dto) {
        try {
            String keycloakId = jwt.getSubject();
            User user = userService.completeProfile(keycloakId, dto);
            return ResponseEntity.ok(Map.of(
                    "message", "Profil complété",
                    "role", user.getRole().name(),
                    "profileComplete", user.isProfileComplete()
            ));
        } catch (RuntimeException e) {
            log.error("Erreur complete-profile : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me/profile-complete")
    public ResponseEntity<?> profileComplete(@AuthenticationPrincipal Jwt jwt) {
        User user = this.userFinderService.getUserByKeycloakId(jwt.getSubject());
        return ResponseEntity.ok(Map.of("complete", user.isProfileComplete()));
    }


}