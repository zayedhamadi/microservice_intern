package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
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

    /**
     * Verrou optimiste (BUG FIX : absent auparavant).
     */
    @Version
    Long version;

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

    /**
     * BUG FIX (race condition) : clé non nulle UNIQUEMENT quand l'entretien
     * est actif (PLANIFIE ou REPORTE), au format
     * "{applicationId}|{type}|{cycleCandidature}". Combinée à l'index
     * unique sparse ci-dessous, elle garantit — de façon atomique côté
     * base de données, contrairement à un simple "exists puis save" côté
     * application — qu'il ne peut jamais exister deux entretiens actifs du
     * même type pour la même candidature/cycle, même en cas d'appels
     * concurrents. Doit être vidée (null) dès que l'entretien quitte l'état
     * actif (TERMINE, ANNULE, ABSENT) : voir InterviewService.
     */
    @Indexed(unique = true, sparse = true)
    String activeSlotKey;

    /**
     * BUG FIX : numéro de cycle de candidature (voir Application#cycleCandidature)
     * auquel cet entretien est rattaché. Évite qu'un entretien d'un dépôt
     * précédent (candidature retirée puis redéposée) ne se mélange avec le
     * cycle courant dans les listings RH.
     */
    Integer cycleCandidature;
}