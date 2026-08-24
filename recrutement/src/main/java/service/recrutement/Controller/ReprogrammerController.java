package service.recrutement.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import service.recrutement.Entity.dto.DemanderReportDto;
import service.recrutement.Entity.dto.ReprogrammerDto;
import service.recrutement.Entity.dto.TraiterDemandeReportDto;
import service.recrutement.Security.JwtExtractService;
import service.recrutement.Service.ReprogrammerService;

import java.util.List;

@RequestMapping("/rh/api/entretiens/")
@RestController
@RequiredArgsConstructor
public class ReprogrammerController {

    private final ReprogrammerService reprogrammerService;
    private final JwtExtractService jwtExtractService;

    private boolean isRH(Authentication auth) {
        return jwtExtractService.hasRole(auth, "ROLE_RH");
    }


    @PostMapping("{interviewId}/reprogrammer/demande")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ReprogrammerDto> demanderParCandidat(
            @PathVariable String interviewId,
            @Valid @RequestBody DemanderReportDto dto,
            Authentication auth) {
        String candidatKeycloakId = jwtExtractService.extractKeycloakId(auth);
        return ResponseEntity.ok(reprogrammerService.demanderParCandidat(interviewId, dto, candidatKeycloakId));
    }

    @PostMapping("{interviewId}/reprogrammer/proposition")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<ReprogrammerDto> proposerParIntervenant(
            @PathVariable String interviewId,
            @Valid @RequestBody DemanderReportDto dto,
            Authentication auth) {
        String auteurKeycloakId = jwtExtractService.extractKeycloakId(auth);
        return ResponseEntity.ok(
                reprogrammerService.proposerParIntervenant(interviewId, dto, auteurKeycloakId, isRH(auth)));
    }

    @PostMapping("{interviewId}/reprogrammer/reactivation-absence")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ReprogrammerDto> demanderReactivationApresAbsence(
            @PathVariable String interviewId,
            @Valid @RequestBody DemanderReportDto dto,
            Authentication auth) {
        String candidatKeycloakId = jwtExtractService.extractKeycloakId(auth);
        return ResponseEntity.ok(
                reprogrammerService.demanderReactivationApresAbsence(interviewId, dto, candidatKeycloakId));
    }

    // ==================== TRAITEMENT ====================

    @PatchMapping("{demandeId}/accepter")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE','CANDIDAT')")
    public ResponseEntity<ReprogrammerDto> accepter(@PathVariable String demandeId, Authentication auth) {
        String auteurKeycloakId = jwtExtractService.extractKeycloakId(auth);
        return ResponseEntity.ok(reprogrammerService.accepterDemande(demandeId, auteurKeycloakId, isRH(auth)));
    }

    @PatchMapping("{demandeId}/refuser")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE','CANDIDAT')")
    public ResponseEntity<ReprogrammerDto> refuser(
            @PathVariable String demandeId,
            @RequestBody TraiterDemandeReportDto dto,
            Authentication auth) {
        String auteurKeycloakId = jwtExtractService.extractKeycloakId(auth);
        return ResponseEntity.ok(
                reprogrammerService.refuserDemande(demandeId, dto.getCommentaire(), auteurKeycloakId, isRH(auth)));
    }

    // ==================== LECTURE ====================

    @GetMapping("{interviewId}/reprogrammer")
    @PreAuthorize("hasAnyRole('CANDIDAT','RH','EMPLOYEE')")
    public ResponseEntity<List<ReprogrammerDto>> getPourEntretien(
            @PathVariable String interviewId, Authentication auth) {
        boolean rhOuEmployee = jwtExtractService.hasRole(auth, "ROLE_RH") || jwtExtractService.hasRole(auth, "ROLE_EMPLOYEE");
        String requesterKeycloakId = jwtExtractService.extractKeycloakId(auth);
        return ResponseEntity.ok(
                reprogrammerService.getDemandesPourEntretien(interviewId, requesterKeycloakId, rhOuEmployee));
    }

    @GetMapping("reprogrammer/mes-demandes")
    @PreAuthorize("hasAnyRole('CANDIDAT','RH','EMPLOYEE')")
    public ResponseEntity<List<ReprogrammerDto>> getMesDemandes(Authentication auth) {
        return ResponseEntity.ok(reprogrammerService.getMesDemandes(jwtExtractService.extractKeycloakId(auth)));
    }

    @GetMapping("reprogrammer/a-traiter")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<List<ReprogrammerDto>> getATraiter(Authentication auth) {
        return ResponseEntity.ok(
                reprogrammerService.getDemandesATraiter(jwtExtractService.extractKeycloakId(auth), isRH(auth)));
    }
}