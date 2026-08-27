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
public class CandidateDashboardStatsDto {

    private String candidatKeycloakId;

    private ApplicationCandidateStatsDto applications;

    private InterviewCandidateStatsDto interviews;

    private PosteCandidateStatsDto postes;

    private CertificationCandidateStatsDto certifications;

    private ReprogrammerCandidateStatsDto reprogrammations;

    private LocalDateTime genereLe;
}