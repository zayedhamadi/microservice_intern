package service.recrutement.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.recrutement.Entity.dto.CandidateDashboardStatsDto;
import service.recrutement.Service.CandidateDashboardService;

@RestController
@RequestMapping("/api/recrutement/candidat/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDAT')")
public class CandidateDashboardController {

    private final CandidateDashboardService candidateDashboardService;

    @GetMapping
    public CandidateDashboardStatsDto getDashboard(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String keycloakId = jwt.getSubject();

        return candidateDashboardService.getDashboard(
                keycloakId
        );
    }
}