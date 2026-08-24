package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.Enum.EtatEntretien;
import service.recrutement.Entity.dto.StatusChange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "applications")
@CompoundIndexes({
        @CompoundIndex(name = "candidat_poste_unique", def = "{'candidatKeycloakId': 1, 'posteRecrutementId': 1}", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Application {

    @Id
    String idApplication;

    /**
     * Verrou optimiste. BUG FIX : sans ce champ, deux écritures concurrentes
     * (ex : RH qui change le statut pendant que le candidat retire sa
     * candidature) s'écrasent silencieusement l'une l'autre ("last write
     * wins"). Spring Data lèvera une OptimisticLockingFailureException en
     * cas de conflit, à traiter côté service/contrôleur.
     */
    @Version
    Long version;

    @Indexed
    String candidatKeycloakId;

    @Indexed
    String posteRecrutementId;


    byte[] cvSnapshot;
    String cvSnapshotFileName;
    String cvSnapshotContentType;

    String lettreMotivationTexte;

    byte[] lettreMotivationPdf;
    String lettreMotivationPdfFileName;

    Double scoreMatching;
    String nomComplet, email, telephone, specialite, formation, commentaireRH;

    String experience;
    Integer anneesExperienceCandidat;

    @Builder.Default
    List<String> competences = new ArrayList<>();

    @Builder.Default
    List<String> langues = new ArrayList<>();

    @Indexed
    ApplicationStatus statut;

    /**
     * @deprecated non maintenu par le workflow actuel (ApplicationStatus
     * porte désormais cette information via EN_ENTRETIEN_RH /
     * EN_ENTRETIEN_TECHNIQUE / EN_ENTRETIEN_FINAL). Conservé uniquement
     * pour compatibilité binaire avec l'existant. À supprimer dès que vous
     * avez confirmé qu'aucun autre composant (front, autre microservice,
     * export) ne le lit ou ne l'écrit.
     */
    @Deprecated
    EtatEntretien etatEntretien;

    LocalDate dateCandidature;
    LocalDateTime dateDernierChangementStatut;

    Boolean entretienPlanifie;
    LocalDateTime dateEntretien;

    @Builder.Default
    Map<String, Double> scoreDetails = new LinkedHashMap<>();

    @Builder.Default
    List<StatusChange> historiqueStatuts = new ArrayList<>();

    /**
     * BUG FIX : incrémenté à chaque redépôt après un retrait
     * (RETIRE -> EN_ATTENTE, voir ApplyService#prepareApplicationForSubmission).
     * Comme idApplication est réutilisé lors d'un redépôt, ce compteur
     * permet à InterviewService de distinguer les entretiens du cycle
     * courant de ceux d'un cycle précédent et d'éviter qu'ils se
     * mélangent dans l'UI du RH.
     */
    @Builder.Default
    Integer cycleCandidature = 1;
}