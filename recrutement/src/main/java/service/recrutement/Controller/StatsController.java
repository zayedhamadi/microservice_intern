package service.recrutement.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.recrutement.Entity.dto.*;
import service.recrutement.Service.StatsService;

@PreAuthorize("hasRole('RH')")
@RestController
@RequestMapping("/api/recrutement/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;


    @GetMapping("/applications")
    public ApplicationStatsDto getApplicationsStats() {
        return statsService.getApplicationStats();
    }

    @GetMapping("/interviews")
    public InterviewsStatsDto getInterviewsStats() {
        return statsService.getInterviewsStats();
    }

    @GetMapping("/dashboard")
    public DashboardStatsDto getDashboard() {
        return statsService.getDashboardStats();
    }

    @GetMapping("/postes")
    public PostesStatsDto getPostesStats() {
        return statsService.getPostesStats();
    }

    @GetMapping("/reprogrammations")
    public ReprogrammerStatsDto getReprogrammerStats() {
        return statsService.getReprogrammerStats();
    }
}