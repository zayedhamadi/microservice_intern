package service.recrutement.Entity.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import service.recrutement.Entity.Enum.ApplicationStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangerStatutDto {
    ApplicationStatus nouveauStatut;
    String commentaireRH;
}