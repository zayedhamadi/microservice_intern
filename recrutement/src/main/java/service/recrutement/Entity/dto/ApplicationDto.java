package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import service.recrutement.Entity.Enum.ApplicationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDto {
    private String idApplication;
    private String candidatKeycloakId;
    private String posteRecrutementId;

    private String cvSnapshotFileName;
    private String lettreMotivationTexte;
    private boolean lettreMotivationPdfPresente;
    private String lettreMotivationPdfFileName;

    private String nomComplet;
    private String email;
    private String telephone;
    private String specialite;
    private String formation;
    private String commentaireRH;

    private String experience;
    private Integer anneesExperienceCandidat;
    private List<String> competences;
    private List<String> langues;

    private ApplicationStatus statut;

    private LocalDate dateCandidature;
    private LocalDateTime dateDernierChangementStatut;

    private Double scoreMatching;
    private List<StatusChange> historiqueStatuts;
}