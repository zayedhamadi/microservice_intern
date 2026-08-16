package service.recrutement.Entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import service.recrutement.Entity.Enum.InterviewResult;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ResultatEntretienDto {

    @NotNull(message = "Le résultat de l'entretien est obligatoire")
    private InterviewResult resultat;

    private String notes;
}