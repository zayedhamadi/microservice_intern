package service.recrutement.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import service.recrutement.Entity.Enum.InterviewStatus;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.dto.InterviewDto;
import service.recrutement.Entity.dto.InterviewStatsDto;
import service.recrutement.Entity.dto.PageResponseDto;
import service.recrutement.Security.JwtExtractService;
import service.recrutement.Service.CandidatService;

import java.util.List;

@RestController
    @RequestMapping("/candidat/api/entretiens")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDAT')")
public class CandidatInterviewController {

    private final CandidatService candidatService;
    private final JwtExtractService jwtExtractService;

    @GetMapping
    public ResponseEntity<PageResponseDto<InterviewDto>> getMesEntretiens(
            @RequestParam(required = false) InterviewType type,
            @RequestParam(required = false) InterviewStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            Authentication auth) {

        String candidatId = jwtExtractService.extractKeycloakId(auth);
        return ResponseEntity.ok(
                candidatService.getMyListInterview(candidatId, type, status, search, page, size, sortBy, sortDir)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewDto> getMonEntretien(@PathVariable String id, Authentication auth) {
        String candidatId = jwtExtractService.extractKeycloakId(auth);
        return ResponseEntity.ok(candidatService.getMyInterviewById(id, candidatId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<InterviewDto>> getMesEntretiensParType(
            @PathVariable InterviewType type, Authentication auth) {
        String candidatId = jwtExtractService.extractKeycloakId(auth);
        return ResponseEntity.ok(candidatService.getMyInterviewsByType(candidatId, type));
    }

    @GetMapping("/stats")
    public ResponseEntity<List<InterviewStatsDto>> getMesStats(Authentication auth) {
        String candidatId = jwtExtractService.extractKeycloakId(auth);
        return ResponseEntity.ok(candidatService.getMyInterviewStats(candidatId));
    }

    @GetMapping("/prochain")
    public ResponseEntity<InterviewDto> getProchainEntretien(Authentication auth) {
        String candidatId = jwtExtractService.extractKeycloakId(auth);
        InterviewDto next = candidatService.getMyNextUpcomingInterview(candidatId);
        return next != null ? ResponseEntity.ok(next) : ResponseEntity.noContent().build();
    }

    @GetMapping("/candidatures/etat")
    public ResponseEntity<List<InterviewStatsDto>> getEtatCandidatures(Authentication auth) {
        String candidatId = jwtExtractService.extractKeycloakId(auth);
        return ResponseEntity.ok(candidatService.getMyApplicationStatusBreakdown(candidatId));
    }
}