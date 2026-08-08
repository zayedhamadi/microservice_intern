package service.recrutement.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.dto.InterviewDto;
import service.recrutement.Entity.dto.PlanifierEntretienDto;
import service.recrutement.Entity.dto.ResultatEntretienDto;
import service.recrutement.Service.InterviewService;

import java.util.List;

@RestController
@RequestMapping("/rh/api")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    // =========================================================
    // PLANIFICATION — un endpoint dédié par étape pour que le rôle
    // Spring Security autorisé corresponde exactement à qui mène l'entretien.
    // =========================================================

    @PostMapping("/candidatures/{id}/entretiens/rh-initial")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<InterviewDto> planifierEntretienRHInitial(
            @PathVariable String id, @RequestBody PlanifierEntretienDto dto, Authentication authentication) {
        return ResponseEntity.ok(interviewService.planifierEntretien(
                id, InterviewType.RH_INITIAL, dto, extractKeycloakId(authentication)));
    }

    @PostMapping("/candidatures/{id}/entretiens/technique")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<InterviewDto> planifierEntretienTechnique(
            @PathVariable String id, @RequestBody PlanifierEntretienDto dto, Authentication authentication) {
        return ResponseEntity.ok(interviewService.planifierEntretien(
                id, InterviewType.TECHNIQUE, dto, extractKeycloakId(authentication)));
    }

    @PostMapping("/candidatures/{id}/entretiens/rh-final")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<InterviewDto> planifierEntretienRHFinal(
            @PathVariable String id, @RequestBody PlanifierEntretienDto dto, Authentication authentication) {
        return ResponseEntity.ok(interviewService.planifierEntretien(
                id, InterviewType.RH_FINAL, dto, extractKeycloakId(authentication)));
    }

    // =========================================================
    // RÉSULTAT
    // =========================================================

    /**
     * RH et EMPLOYEE peuvent tous deux enregistrer un résultat ici ; le service ne
     * revérifie pas que le rôle de l'appelant correspond au type de l'entretien.
     * Si tu veux serrer cette règle, ajoute un contrôle dans InterviewService en
     * comparant interview.getType() au rôle de `authentication` avant d'enregistrer.
     */
    @PatchMapping("/entretiens/{interviewId}/resultat")
    @PreAuthorize("hasAnyRole('RH', 'EMPLOYEE')")
    public ResponseEntity<InterviewDto> enregistrerResultat(
            @PathVariable String interviewId, @RequestBody ResultatEntretienDto dto, Authentication authentication) {
        return ResponseEntity.ok(interviewService.enregistrerResultat(
                interviewId, dto, extractKeycloakId(authentication)));
    }

    // =========================================================
    // CONSULTATION — candidat (sa propre candidature), RH, EMPLOYEE
    // =========================================================

    @GetMapping("/candidatures/{id}/entretiens")
    @PreAuthorize("hasAnyRole('CANDIDAT', 'RH', 'EMPLOYEE')")
    public ResponseEntity<List<InterviewDto>> getEntretiensPourCandidature(
            @PathVariable String id, Authentication authentication) {
        boolean isRhOuEmployee = hasRole(authentication, "ROLE_RH") || hasRole(authentication, "ROLE_EMPLOYEE");
        return ResponseEntity.ok(interviewService.getEntretiensPourCandidature(
                id, extractKeycloakId(authentication), isRhOuEmployee));
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String extractKeycloakId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getSubject();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}