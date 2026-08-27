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
public class ApplicationCandidateStatsDto {

    private long total;

    private Map<String, Long> parStatut;

    private long ceMois;

    private long acceptees;

    private long rejetees;

    private long enCours;

    private long retirees;

    private double tauxAcceptation;

    private Double scoreMatchingMoyen;

    private List<MonthDataDto> serieMensuelle;
}