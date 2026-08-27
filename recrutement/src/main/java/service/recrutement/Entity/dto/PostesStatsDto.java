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
public class PostesStatsDto {
    private long total;
    private long ouverts;
    private Map<String, Long> parStatut;
    private Map<String, Long> parDepartement;
    private long expirantSous7Jours;
    private EvolutionDto evolution;
    private List<MonthDataDto> serieMensuelle;
}