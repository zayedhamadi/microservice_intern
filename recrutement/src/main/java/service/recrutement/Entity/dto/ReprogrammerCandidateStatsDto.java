package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReprogrammerCandidateStatsDto {

    private long total;

    private long enAttente;

    private long acceptees;

    private long refusees;

    private Map<String, Long> parStatut;
}