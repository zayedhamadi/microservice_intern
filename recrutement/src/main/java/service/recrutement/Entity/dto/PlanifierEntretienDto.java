package service.recrutement.Entity.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import service.recrutement.Entity.Enum.InterviewMode;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PlanifierEntretienDto {

    @NotNull(message = "Le mode de l'entretien est obligatoire")
    private InterviewMode mode;

    @NotNull(message = "La date de l'entretien est obligatoire")
    @Future(message = "La date de l'entretien doit être dans le futur")
    private LocalDateTime dateEntretien;

    private String lieu;

    private String lienVisio;
}