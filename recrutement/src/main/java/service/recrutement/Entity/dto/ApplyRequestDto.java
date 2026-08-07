package service.recrutement.Entity.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import service.recrutement.Entity.Enum.CvChoice;

/**
 * Requête de candidature avec CV existant (JSON, pas de fichier).
 * Pour le nouveau CV, voir le endpoint multipart dédié dans le contrôleur.
 */
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