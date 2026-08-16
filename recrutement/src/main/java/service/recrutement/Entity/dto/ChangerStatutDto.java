package service.recrutement.Entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import service.recrutement.Entity.Enum.ApplicationStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChangerStatutDto {

    @NotNull(message = "Le nouveau statut est obligatoire")
    private ApplicationStatus nouveauStatut;

    private String commentaireRH;
}