package service.recrutement.Entity.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DemanderReportDto {

    @NotNull(message = "La nouvelle date proposée est obligatoire")
    @Future(message = "La nouvelle date proposée doit être dans le futur")
    private LocalDateTime nouvelleDateProposee;

    @NotBlank(message = "Le motif est obligatoire")
    private String motif;
}