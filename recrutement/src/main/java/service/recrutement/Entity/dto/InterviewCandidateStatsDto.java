package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewCandidateStatsDto {

    private long total;

    private long aVenir;

    private long dansLes7Jours;

    private long termines;

    private long reussis;

    private double tauxReussite;

    private Map<String, Long> parStatut;

    private List<UpcomingInterviewDto> prochains;
}