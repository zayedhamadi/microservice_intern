package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import service.recrutement.Entity.Enum.*;

import java.time.LocalDateTime;

/**
 * Entité unique pour tout entretien, qu'il soit créé librement depuis le
 * calendrier RH (source = LIBRE) ou issu du workflow de candidature
 * (source = CANDIDATURE, applicationId renseigné).
 */
@Document(collection = "interviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Interview {

    @Id
    String idInterview;

    @Indexed
    InterviewSource source;

    @Indexed
    String applicationId;

    @Indexed
    String candidatKeycloakId;

    String candidateName;
    String candidateEmail;

    String posteRecrutement;
    String posteId;

    /** Id Keycloak du recruteur si connu */
    String recruteurKeycloakId;
    /** Nom affiché de l'intervenant (toujours renseigné, même en LIBRE) */
    String interviewerName;

    /** null pour un entretien LIBRE */
    InterviewType type;

    @Indexed
    LocalDateTime dateEntretien;

    /** Fin de l'entretien. Si absent, on considère dateEntretien + 1h. */
    LocalDateTime dateFinEntretien;

    InterviewMode mode;

    String lieu;
    String lienVisio;

    @Indexed
    InterviewStatus statut;

    InterviewResult resultat;

    String notes;

    LocalDateTime dateCreation;
    LocalDateTime dateModification;
}