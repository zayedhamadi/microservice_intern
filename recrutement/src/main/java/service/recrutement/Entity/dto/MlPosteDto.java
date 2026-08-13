package service.recrutement.Entity.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlPosteDto {
    private String idPosteRecrutement;
    private List<String> competencesRequises;
    private List<String> languesRequises;
    private Integer anneesExperienceMin;
    private String niveauEtudeRequis;
    private String typeContrat;
    private String workType;
    private String lieu;
    private Double salaire;
}