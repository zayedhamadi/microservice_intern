package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import service.recrutement.Entity.Enum.TypeContrat;
import service.recrutement.Entity.Enum.WorkType;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteCandidateItemDto {

    private String idPosteRecrutement;

    private String titre;

    private String departementNom;

    private TypeContrat typeContrat;

    private WorkType workType;

    private String lieu;

    private Long salaire;

    private Integer nombrePostes;

    private LocalDate datePosteRecrutement;

    private LocalDate dateExpirationPosteRecrutement;
}