package service.recrutement.Entity.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.Enum.EtatEntretien;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de réponse — projection sûre de Application, sans le contenu binaire du CV
 * (pour ne pas alourdir les listes) ni les annotations de persistance Mongo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationDto {
    String idApplication;
    String candidatKeycloakId;
    String posteRecrutementId;

    String cvSnapshotFileName;
    String lettreMotivationTexte;

    String nomComplet, email, telephone, specialite, formation, commentaireRH;
    String experience;
    Integer anneesExperienceCandidat;

    List<String> competences;
    List<String> langues;

    ApplicationStatus statut;
    EtatEntretien etatEntretien;

    LocalDate dateCandidature;
    LocalDateTime dateDernierChangementStatut;

    Boolean entretienPlanifie;
    LocalDateTime dateEntretien;

    Double scoreMatching;
}