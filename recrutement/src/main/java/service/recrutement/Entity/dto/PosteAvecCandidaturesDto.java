package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import service.recrutement.Entity.Enum.*;


import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteAvecCandidaturesDto {

    private String idPosteRecrutement;
    private String titre;
    private String departementNom;
    private TypeContrat typeContrat;
    private WorkType workType;
    private StatusPosteRecrutement status;
    private String lieu;
    private Double salaire;
    private Integer nombrePostes;

    private String datePosteRecrutement;
    private String dateExpirationPosteRecrutement;

    private long nombreCandidatures;
    private long nombreEnAttente;
    private long nombreEnEntretienRH;
    private long nombreEnEntretienTechnique;
    private long nombreAcceptees;
    private long nombreRefusees;

    private List<String> competencesRequises;
}