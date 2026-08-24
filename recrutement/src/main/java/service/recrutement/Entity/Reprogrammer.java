package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import service.recrutement.Entity.Enum.DemandeReportStatus;
import service.recrutement.Entity.Enum.TypeDemandeReport;

import java.time.LocalDateTime;

@Document(collection = "reprogrammer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reprogrammer {

    @Id
    String idReprogrammer;

    @Version
    Long version;

    @Indexed
    String interviewId;

    @Indexed
    String applicationId;

    TypeDemandeReport type;

    @Indexed
    String demandeurKeycloakId;

    /**
     * Keycloak id de la personne qui doit traiter la demande.
     * null = cible générique "n'importe quel RH" (cas REACTIVATION_APRES_ABSENCE).
     */
    String cibleKeycloakId;

    LocalDateTime ancienneDate;
    LocalDateTime nouvelleDateProposee;

    String motif;

    @Indexed
    DemandeReportStatus statut;

    String commentaireTraitement;
    String traiteParKeycloakId;

    LocalDateTime dateCreation;
    LocalDateTime dateTraitement;
}