package service.recrutement.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.dto.ApplicationDto;
import service.recrutement.Entity.dto.ApplyRequestDto;
import service.recrutement.Entity.dto.ChangerStatutDto;
import service.recrutement.Service.ApplyService;
import service.recrutement.Security.JwtExtractService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/rh/api/candidatures")
@RequiredArgsConstructor
public class ApplyController {

    private final ApplyService applyService;
    private final JwtExtractService jwtExtractService;

    private static final String ROLE_RH = "ROLE_RH";

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApplicationDto> postulerAvecCvExistant(
            @RequestParam String idPosteRecrutement,
            @RequestParam(required = false) String lettreMotivationTexte,
            @RequestParam(required = false) MultipartFile lettreMotivationPdf,
            Authentication authentication) {

        String candidatKeycloakId = this.jwtExtractService.extractKeycloakId(authentication);
        ApplyRequestDto dto = ApplyRequestDto.builder()
                .idPosteRecrutement(idPosteRecrutement)
                .lettreMotivationTexte(lettreMotivationTexte)
                .build();

        return ResponseEntity.ok(
                applyService.postulerAvecCvExistant(candidatKeycloakId, dto, lettreMotivationPdf));
    }

    @PostMapping(value = "/avec-nouveau-cv", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApplicationDto> postulerAvecNouveauCv(
            @RequestParam String idPosteRecrutement,
            @RequestParam(required = false) String lettreMotivationTexte,
            @RequestParam MultipartFile cv,
            @RequestParam(required = false) MultipartFile lettreMotivationPdf,
            Authentication authentication) {

        String candidatKeycloakId = this.jwtExtractService.extractKeycloakId(authentication);
        return ResponseEntity.ok(applyService.postulerAvecNouveauCv(
                candidatKeycloakId, idPosteRecrutement, cv, lettreMotivationTexte, lettreMotivationPdf));
    }

    /** Modifier une candidature EN_ATTENTE (lettre de motivation, CV, lettre PDF). */
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApplicationDto> modifierCandidature(
            @PathVariable String id,
            @RequestParam(required = false) String lettreMotivationTexte,
            @RequestParam(required = false) MultipartFile cv,
            @RequestParam(required = false) MultipartFile lettreMotivationPdf,
            @RequestParam(required = false, defaultValue = "false") boolean supprimerLettrePdf,
            Authentication authentication) {

        String candidatKeycloakId = this.jwtExtractService.extractKeycloakId(authentication);
        return ResponseEntity.ok(applyService.modifierCandidature(
                id, candidatKeycloakId, lettreMotivationTexte, cv, lettreMotivationPdf, supprimerLettrePdf));
    }

    @GetMapping("/mon-statut/{posteId}")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApplicationDto> getMaCandidaturePourPoste(
            @PathVariable String posteId, Authentication authentication) {
        String candidatKeycloakId = this.jwtExtractService.extractKeycloakId(authentication);
        Optional<ApplicationDto> candidature = applyService.getMaCandidaturePourPoste(candidatKeycloakId, posteId);
        return candidature.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/mes-candidatures")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<List<ApplicationDto>> getMesCandidatures(Authentication authentication) {
        String candidatKeycloakId = this.jwtExtractService.extractKeycloakId(authentication);
        return ResponseEntity.ok(applyService.getMesCandidatures(candidatKeycloakId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<Void> retirerCandidature(@PathVariable String id, Authentication authentication) {
        String candidatKeycloakId = this.jwtExtractService.extractKeycloakId(authentication);
        applyService.retirerCandidature(id, candidatKeycloakId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/lettre-motivation")
    @PreAuthorize("hasAnyRole('CANDIDAT', 'RH')")
    public ResponseEntity<byte[]> telechargerLettreMotivation(
            @PathVariable String id, Authentication authentication) {

        boolean isRH = this.jwtExtractService.hasRole(authentication, ROLE_RH);
        String requesterKeycloakId = this.jwtExtractService.extractKeycloakId(authentication);

        Application application = applyService.getApplicationPourTelechargementLettre(id, requesterKeycloakId, isRH);

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + application.getLettreMotivationPdfFileName() + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(application.getLettreMotivationPdf());
    }

    @GetMapping("/poste/{posteId}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<List<ApplicationDto>> getCandidaturesPourPoste(@PathVariable String posteId) {
        return ResponseEntity.ok(applyService.getCandidaturesPourPoste(posteId));
    }

    @GetMapping("/poste/{posteId}/count")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Long> countCandidaturesPourPoste(@PathVariable String posteId) {
        return ResponseEntity.ok(applyService.countCandidaturesPourPoste(posteId));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<ApplicationDto> changerStatut(
            @PathVariable String id, @RequestBody ChangerStatutDto dto, Authentication authentication) {
        String recruteurKeycloakId = this.jwtExtractService.extractKeycloakId(authentication);
        ApplicationStatus nouveauStatut = dto.getNouveauStatut();
        return ResponseEntity.ok(
                applyService.changerStatutParRH(id, nouveauStatut, dto.getCommentaireRH(), recruteurKeycloakId));
    }

}