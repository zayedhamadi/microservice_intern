package service.recrutement.Entity.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlCandidatDto {
    private String keycloakId;
    private List<String> competences;
    private List<String> langues;
    private Integer anneesExperience;
    private String niveauEtude;
    private String typeContratSouhaite;
    private String lieu;
    private Double salaireAttendu;
    private List<String> certifications;
}