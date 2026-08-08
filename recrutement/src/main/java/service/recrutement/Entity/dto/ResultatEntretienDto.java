package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import service.recrutement.Entity.Enum.InterviewResult;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultatEntretienDto {
    private InterviewResult resultat;
    private String notes;
}