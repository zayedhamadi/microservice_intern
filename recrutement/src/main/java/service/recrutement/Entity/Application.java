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