package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import service.recrutement.Entity.Enum.StatusPosteRecrutement;
import service.recrutement.Entity.Enum.TypeContrat;
import service.recrutement.Entity.Enum.WorkType;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteRecrutementDto {

    private String idPosteRecrutement;

    private String recruteurKeycloakId;

    private String titre;
    private String description;
    private String profilDemandeOfPoste;

    private List<String> competencesRequises;
    private List<String> languesRequises;

    private Integer anneesExperienceMin;
    private String niveauEtudeRequis;

    private TypeContrat typeContrat;
    private StatusPosteRecrutement status;
    private WorkType workType;

    private String lieu;
    private Long salaire;
    private Integer nombrePostes;

    private String departementNom;

    private LocalDate datePosteRecrutement;
    private LocalDate dateExpirationPosteRecrutement;
}