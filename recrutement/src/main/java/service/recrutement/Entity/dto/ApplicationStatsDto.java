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
public class ApplicationStatsDto {
    private long total;
    private Map<String, Long> parStatut;
    private EvolutionDto evolution;
    private double tauxAcceptation;
    private double tauxRejet;
    private List<MonthDataDto> serieMensuelle;
}