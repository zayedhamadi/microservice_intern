package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolutionDto {
    private long total;
    private long ceMois;
    private long moisDernier;
    private double variationPourcent;
}