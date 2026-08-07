package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.Enum.EtatEntretien;

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

    @Indexed
    String candidatKeycloakId;

    @Indexed
    String posteRecrutementId;

    // Snapshot du CV au moment de la candidature (immuable même si le candidat
    // met à jour son CV par défaut plus tard — traçabilité).
    byte[] cvSnapshot;
    String cvSnapshotFileName;
    String cvSnapshotContentType;

    // Lettre de motivation texte libre (chemin principal du formulaire Angular)
    String lettreMotivationTexte;

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

    EtatEntretien etatEntretien;

    LocalDate dateCandidature;
    LocalDateTime dateDernierChangementStatut;

    Boolean entretienPlanifie;
    LocalDateTime dateEntretien;

    @Builder.Default
    Map<String, Double> scoreDetails = new LinkedHashMap<>();

    @Builder.Default
    List<StatusChange> historiqueStatuts = new ArrayList<>();
}