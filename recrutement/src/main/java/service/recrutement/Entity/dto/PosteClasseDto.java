package service.recrutement.Entity.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteClasseDto {
    private String idPosteRecrutement;
    private String titre;
    private String departementNom;
    private Double score;
}