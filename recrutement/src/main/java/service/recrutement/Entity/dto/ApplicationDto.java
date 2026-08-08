package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.StatusChange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * NOTE : par rapport à l'ancienne version, `etatEntretien`, `entretienPlanifie`
 * et `dateEntretien` (uniques, pensés pour un seul entretien) ont été retirés.
 * L'état détaillé du processus d'entretien vit maintenant dans la collection
 * Interview (voir InterviewController / GET .../entretiens), et `historiqueStatuts`
 * donne au candidat une vue chronologique complète de sa candidature.
 */
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