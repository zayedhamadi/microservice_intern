package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private ApplicationStatsDto applications;
    private InterviewsStatsDto interviews;
    private PostesStatsDto postes;
    private ReprogrammerStatsDto reprogrammations;
    private LocalDateTime genereLe;
}