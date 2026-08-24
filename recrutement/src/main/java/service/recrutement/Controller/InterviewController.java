package service.recrutement.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.dto.*;
import service.recrutement.Security.JwtExtractService;
import service.recrutement.Service.InterviewService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final JwtExtractService jwtExtractService;

    @GetMapping("/rh/api/entretiens/mes-candidats-techniques")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<CandidatEntretienTechniqueDto>> getMesCandidatsTechniques(Authentication auth) {
        return ResponseEntity.ok(interviewService.getMesCandidatsTechniques(jwtExtractService.extractKeycloakId(auth)));
    }

    @GetMapping("/interviews/mes-entretiens")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<List<InterviewDto>> getMesEntretiens(Authentication auth) {
        return ResponseEntity.ok(interviewService.getMesEntretiens(jwtExtractService.extractKeycloakId(auth)));
    }

    @GetMapping("/interviews")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<List<InterviewDto>> getAll() {
        return ResponseEntity.ok(interviewService.getAll());
    }

    @GetMapping("/interviews/{id}")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<InterviewDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(interviewService.getById(id));
    }

    @PostMapping("/interviews")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<InterviewDto> create(@RequestBody InterviewDto dto, Authentication auth) {
        return ResponseEntity.ok(interviewService.createLibre(dto, jwtExtractService.extractKeycloakId(auth)));
    }

    @PutMapping("/interviews/{id}")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<InterviewDto> update(@PathVariable String id, @RequestBody InterviewDto dto, Authentication auth) {
        return ResponseEntity.ok(interviewService.updateLibre(id, dto, jwtExtractService.extractKeycloakId(auth)));
    }

    @DeleteMapping("/interviews/{id}")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        interviewService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rh/api/entretiens")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<List<InterviewDto>> getAllRecrutement() {
        return ResponseEntity.ok(interviewService.getAllCandidature());
    }

    @GetMapping("/rh/api/entretiens/techniques-par-poste")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<List<PosteEntretiensTechniquesDto>> getEntretiensTechniquesParPoste() {
        return ResponseEntity.ok(interviewService.getEntretiensTechniquesParPoste());
    }

    @GetMapping("/interviews/stats")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<List<InterviewStatsDto>> stats() {
        return ResponseEntity.ok(interviewService.getStats());
    }

    // ==================== Workflow candidature ====================

    @PostMapping("/rh/api/candidatures/{id}/entretiens/rh-initial")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<InterviewDto> rhInitial(@PathVariable String id, @Valid @RequestBody PlanifierEntretienDto dto, Authentication auth) {
        return ResponseEntity.ok(interviewService.planifierEntretien(id, InterviewType.RH_INITIAL, dto, jwtExtractService.extractKeycloakId(auth)));
    }

    @PostMapping("/rh/api/candidatures/{id}/entretiens/technique")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<InterviewDto> tech(@PathVariable String id, @Valid @RequestBody PlanifierEntretienDto dto, Authentication auth) {
        return ResponseEntity.ok(interviewService.planifierEntretien(id, InterviewType.TECHNIQUE, dto, jwtExtractService.extractKeycloakId(auth)));
    }

    @PostMapping("/rh/api/candidatures/{id}/entretiens/rh-final")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<InterviewDto> rhFinal(@PathVariable String id, @Valid @RequestBody PlanifierEntretienDto dto, Authentication auth) {
        return ResponseEntity.ok(interviewService.planifierEntretien(id, InterviewType.RH_FINAL, dto, jwtExtractService.extractKeycloakId(auth)));
    }
@PatchMapping("/rh/api/entretiens/{interviewId}/annuler")
@PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
public ResponseEntity<InterviewDto> annuler(
        @PathVariable String interviewId,
        @RequestBody(required = false) AnnulerEntretienDto dto, // crée ce petit DTO si besoin, ou passe juste un @RequestParam String motif
        Authentication auth) {

    boolean isRH = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_RH"));

    String motif = dto != null ? dto.getMotif() : null;

    return ResponseEntity.ok(interviewService.annulerEntretien(
            interviewId,
            motif,
            jwtExtractService.extractKeycloakId(auth),
            isRH));
}

@PatchMapping("/rh/api/entretiens/{interviewId}/absent")
@PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
public ResponseEntity<InterviewDto> marquerAbsent(
        @PathVariable String interviewId,
        Authentication auth) {

    boolean isRH = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_RH"));

    return ResponseEntity.ok(interviewService.marquerAbsent(
            interviewId,
            jwtExtractService.extractKeycloakId(auth),
            isRH));
}

@PatchMapping("/rh/api/entretiens/{interviewId}/reporter")
@PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
public ResponseEntity<InterviewDto> reporter(
        @PathVariable String interviewId,
        @Valid @RequestBody ReporterEntretienDto dto, // { LocalDateTime nouvelleDate }
        Authentication auth) {

    boolean isRH = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_RH"));

    return ResponseEntity.ok(interviewService.reporterEntretien(
            interviewId,
            dto.getNouvelleDate(),
            jwtExtractService.extractKeycloakId(auth),
            isRH));
}
 @PatchMapping("/rh/api/entretiens/{interviewId}/resultat")
@PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
public ResponseEntity<InterviewDto> resultat(
        @PathVariable String interviewId,
        @Valid @RequestBody ResultatEntretienDto dto,
        Authentication auth) {

    boolean isRH = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_RH"));

    return ResponseEntity.ok(interviewService.enregistrerResultat(
            interviewId,
            dto,
            jwtExtractService.extractKeycloakId(auth),
            isRH));
}

    @GetMapping("/rh/api/candidatures/{id}/entretiens")
    @PreAuthorize("hasAnyRole('CANDIDAT','RH','EMPLOYEE')")
    public ResponseEntity<List<InterviewDto>> getEnt(@PathVariable String id, Authentication auth) {
        boolean rh = jwtExtractService.hasRole(auth, "ROLE_RH") || jwtExtractService.hasRole(auth, "ROLE_EMPLOYEE");
        return ResponseEntity.ok(interviewService.getEntretiensPourCandidature(id, jwtExtractService.extractKeycloakId(auth), rh));
    }
}