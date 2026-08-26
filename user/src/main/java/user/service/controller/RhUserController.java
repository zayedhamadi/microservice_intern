package user.service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import user.service.Entity.User;
import user.service.Dto.UserProfileDto;
import user.service.Serivce.UserCommun.UserFinderService;

import java.util.Base64;

@RestController
@RequestMapping("/rh")
@RequiredArgsConstructor
public class RhUserController {

    private final UserFinderService userFinderService;

    @GetMapping("/{keycloakId}/profil")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<UserProfileDto> getProfilForRH(@PathVariable String keycloakId) {
        User user = userFinderService.getUserByKeycloakId(keycloakId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String imageBase64 = null;
        if (user.getImage() != null && user.getImage().length > 0) {
            imageBase64 = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(user.getImage());
        }

        UserProfileDto dto = UserProfileDto.builder()
                .keycloakId(user.getKeycloakId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .adresse(user.getAdresse())
                .numTel(user.getNum_Tel())
                .specialiteEtude(user.getSpecialiteEtude())
                .niveauEtude(user.getNiveauEtude() != null ? user.getNiveauEtude().name() : null)
                .universiteEtude(user.getUniversiteEtude())
                .anneesExperience(user.getAnneesExperience())
                .linkedin(user.getLinkedin())
                .matricule(user.getMatricule())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .compte(user.getEtatCompte() != null ? user.getEtatCompte().name() : null)
                .imageBase64(imageBase64)
                .build();

        return ResponseEntity.ok(dto);
    }
}