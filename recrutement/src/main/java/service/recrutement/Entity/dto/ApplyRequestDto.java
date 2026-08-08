package service.recrutement.Entity.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import service.recrutement.Entity.Enum.CvChoice;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplyRequestDto {
    String idPosteRecrutement;
    CvChoice cvChoice;
    String lettreMotivationTexte;
}