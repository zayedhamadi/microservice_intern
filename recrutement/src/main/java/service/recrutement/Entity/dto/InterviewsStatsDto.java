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
public class InterviewsStatsDto {
    private long total;
    private Map<String, Long> parStatut;
    private Map<String, Long> parType;
    private long aVenir;
    private long dansLes7Jours;
    private double tauxReussite;
    private List<MonthDataDto> serieMensuelle;
}