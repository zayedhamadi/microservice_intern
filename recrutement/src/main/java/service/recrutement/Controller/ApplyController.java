package service.recrutement.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.dto.ApplicationDto;
import service.recrutement.Entity.dto.ApplyRequestDto;
import service.recrutement.Entity.dto.ChangerStatutDto;
import service.recrutement.Security.JwtExtractService;
import service.recrutement.Service.ApplyService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/rh/api/candidatures")
@RequiredArgsConstructor
public class ApplyController {

    private static final String ROLE_RH = "ROLE_RH";

    private final ApplyService applyService;
    private final JwtExtractService jwtExtractService;

    @GetMapping("/poste/{posteId}/classees")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<List<ApplicationDto>>
    getCandidaturesClasseesPourPoste(
            @PathVariable String posteId) {

        return ResponseEntity.ok(
                applyService.getCandidaturesClasseesPourPoste(posteId)
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApplicationDto> postulerAvecCvExistant(
            @RequestParam String idPosteRecrutement,
            @RequestParam(required = false)
            String lettreMotivationTexte,
            @RequestParam(required = false)
            MultipartFile lettreMotivationPdf,
            Authentication authentication) {

        String candidatKeycloakId =
                jwtExtractService.extractKeycloakId(authentication);

        ApplyRequestDto dto = ApplyRequestDto.builder()
                .idPosteRecrutement(idPosteRecrutement)
                .lettreMotivationTexte(lettreMotivationTexte)
                .build();

        return ResponseEntity.ok(
                applyService.postulerAvecCvExistant(
                        candidatKeycloakId,
                        dto,
                        lettreMotivationPdf
                )
        );
    }

    @PostMapping(
            value = "/avec-nouveau-cv",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApplicationDto> postulerAvecNouveauCv(
            @RequestParam String idPosteRecrutement,
            @RequestParam(required = false)
            String lettreMotivationTexte,
            @RequestParam MultipartFile cv,
            @RequestParam(required = false)
            MultipartFile lettreMotivationPdf,
            Authentication authentication) {

        String candidatKeycloakId =
                jwtExtractService.extractKeycloakId(authentication);

        return ResponseEntity.ok(
                applyService.postulerAvecNouveauCv(
                        candidatKeycloakId,
                        idPosteRecrutement,
                        cv,
                        lettreMotivationTexte,
                        lettreMotivationPdf
                )
        );
    }

    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApplicationDto> modifierCandidature(
            @PathVariable String id,
            @RequestParam(required = false)
            String lettreMotivationTexte,
            @RequestParam(required = false)
            MultipartFile cv,
            @RequestParam(required = false)
            MultipartFile lettreMotivationPdf,
            @RequestParam(
                    required = false,
                    defaultValue = "false"
            )
            boolean supprimerLettrePdf,
            Authentication authentication) {

        String candidatKeycloakId =
                jwtExtractService.extractKeycloakId(authentication);

        return ResponseEntity.ok(
                applyService.modifierCandidature(
                        id,
                        candidatKeycloakId,
                        lettreMotivationTexte,
                        cv,
                        lettreMotivationPdf,
                        supprimerLettrePdf
                )
        );
    }

    @GetMapping("/mon-statut/{posteId}")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApplicationDto> getMaCandidaturePourPoste(
            @PathVariable String posteId,
            Authentication authentication) {

        String candidatKeycloakId =
                jwtExtractService.extractKeycloakId(authentication);

        Optional<ApplicationDto> candidature =
                applyService.getMaCandidaturePourPoste(
                        candidatKeycloakId,
                        posteId
                );

        return candidature
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/mes-candidatures")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<List<ApplicationDto>> getMesCandidatures(
            Authentication authentication) {

        String candidatKeycloakId =
                jwtExtractService.extractKeycloakId(authentication);

        return ResponseEntity.ok(
                applyService.getMesCandidatures(candidatKeycloakId)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<Void> retirerCandidature(
            @PathVariable String id,
            Authentication authentication) {

        String candidatKeycloakId =
                jwtExtractService.extractKeycloakId(authentication);

        applyService.retirerCandidature(id, candidatKeycloakId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/lettre-motivation")
    @PreAuthorize("hasAnyRole('CANDIDAT', 'RH')")
    public ResponseEntity<byte[]> telechargerLettreMotivation(
            @PathVariable String id,
            Authentication authentication) {

        boolean isRh =
                jwtExtractService.hasRole(authentication, ROLE_RH);

        String requesterKeycloakId =
                jwtExtractService.extractKeycloakId(authentication);

        Application application =
                applyService.getApplicationPourTelechargementLettre(
                        id,
                        requesterKeycloakId,
                        isRh
                );

        return ResponseEntity.ok()
                .header(
                        "Content-Disposition",
                        "inline; filename=\""
                                + application
                                .getLettreMotivationPdfFileName()
                                + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(application.getLettreMotivationPdf());
    }

    @GetMapping("/poste/{posteId}")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<List<ApplicationDto>> getCandidaturesPourPoste(
            @PathVariable String posteId) {

        return ResponseEntity.ok(
                applyService.getCandidaturesPourPoste(posteId)
        );
    }

    @GetMapping("/poste/{posteId}/count")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Long> countCandidaturesPourPoste(
            @PathVariable String posteId) {

        return ResponseEntity.ok(
                applyService.countCandidaturesPourPoste(posteId)
        );
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<ApplicationDto> changerStatut(
            @PathVariable String id,
            @Valid @RequestBody ChangerStatutDto dto,
            Authentication authentication) {

        String recruteurKeycloakId =
                jwtExtractService.extractKeycloakId(authentication);

        return ResponseEntity.ok(
                applyService.changerStatutParRH(
                        id,
                        dto.getNouveauStatut(),
                        dto.getCommentaireRH(),
                        recruteurKeycloakId
                )
        );
    }
}