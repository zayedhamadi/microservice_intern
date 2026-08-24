package service.recrutement.Entity.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporterEntretienDto {
    @NotNull(message = "La nouvelle date est obligatoire")
    @Future(message = "La nouvelle date doit être dans le futur")
    private LocalDateTime nouvelleDate;
}