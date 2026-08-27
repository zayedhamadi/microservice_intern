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
public class ReprogrammerStatsDto {
    private long total;
    private long enAttente;
    private Map<String, Long> parStatut;
    private Map<String, Long> parType;
    private List<MonthDataDto> serieMensuelle;
}