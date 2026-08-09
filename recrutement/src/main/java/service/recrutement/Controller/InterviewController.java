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
import service.recrutement.Security.JwtExtractService;
import service.recrutement.Service.InterviewService;

import java.util.List;

@RestController
@RequestMapping("/rh/api")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final JwtExtractService jwtExtractService;



    @PostMapping("/candidatures/{id}/entretiens/rh-initial")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<InterviewDto> planifierEntretienRHInitial(
            @PathVariable String id, @RequestBody PlanifierEntretienDto dto, Authentication authentication) {
        return ResponseEntity.ok(interviewService.planifierEntretien(
                id, InterviewType.RH_INITIAL, dto, jwtExtractService.extractKeycloakId(authentication)));
    }

    @PostMapping("/candidatures/{id}/entretiens/technique")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<InterviewDto> planifierEntretienTechnique(
            @PathVariable String id, @RequestBody PlanifierEntretienDto dto, Authentication authentication) {
        return ResponseEntity.ok(interviewService.planifierEntretien(
                id, InterviewType.TECHNIQUE, dto, jwtExtractService.extractKeycloakId(authentication)));
    }

    @PostMapping("/candidatures/{id}/entretiens/rh-final")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<InterviewDto> planifierEntretienRHFinal(
            @PathVariable String id, @RequestBody PlanifierEntretienDto dto, Authentication authentication) {
        return ResponseEntity.ok(interviewService.planifierEntretien(
                id, InterviewType.RH_FINAL, dto, jwtExtractService.extractKeycloakId(authentication)));
    }


    @PatchMapping("/entretiens/{interviewId}/resultat")
    @PreAuthorize("hasAnyRole('RH', 'EMPLOYEE')")
    public ResponseEntity<InterviewDto> enregistrerResultat(
            @PathVariable String interviewId, @RequestBody ResultatEntretienDto dto, Authentication authentication) {
        return ResponseEntity.ok(interviewService.enregistrerResultat(
                interviewId, dto, jwtExtractService.extractKeycloakId(authentication)));
    }


    @GetMapping("/candidatures/{id}/entretiens")
    @PreAuthorize("hasAnyRole('CANDIDAT', 'RH', 'EMPLOYEE')")
    public ResponseEntity<List<InterviewDto>> getEntretiensPourCandidature(
            @PathVariable String id, Authentication authentication) {
        boolean isRhOuEmployee = jwtExtractService.hasRole(authentication, "ROLE_RH") || jwtExtractService.hasRole(authentication, "ROLE_EMPLOYEE");
        return ResponseEntity.ok(interviewService.getEntretiensPourCandidature(
                id, jwtExtractService.extractKeycloakId(authentication), isRhOuEmployee));
    }



}