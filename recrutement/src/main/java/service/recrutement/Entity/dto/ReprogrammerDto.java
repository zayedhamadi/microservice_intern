package service.recrutement.Entity.dto;

import lombok.*;
import service.recrutement.Entity.Enum.DemandeReportStatus;
import service.recrutement.Entity.Enum.TypeDemandeReport;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class ReprogrammerDto {
    private String id;
    private String interviewId;
    private String applicationId;
    private TypeDemandeReport type;
    private String demandeurKeycloakId;
    private String cibleKeycloakId;
    private String ancienneDate;
    private String nouvelleDateProposee;
    private String motif;
    private DemandeReportStatus statut;
    private String commentaireTraitement;
    private String traiteParKeycloakId;
    private String dateCreation;
    private String dateTraitement;

    // Champs pratiques pour l'affichage front, évite un appel supplémentaire
    private String candidateName;
    private String posteRecrutement;
    private String interviewerName;
}