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
import java.util.List;

@Document(collection = "applications")
@CompoundIndexes({
        // Empêche un candidat de postuler deux fois à la même offre — contrainte au niveau DB, pas juste applicatif
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

    // --- Références (pattern keycloakId/String id, jamais d'objet cross-service) ---
    @Indexed
    String candidatKeycloakId;

    @Indexed
    String posteRecrutementId;

    // --- Snapshot du CV au moment de la candidature ---
    // Volontairement une copie, pas une référence vers FileUser : si le candidat change son CV
    // après coup, le recruteur doit continuer à voir la version qu'il a réellement évaluée.
    // ATTENTION perf : exclure ce champ des projections de liste (findAll côté RH) pour éviter
    // de charger les bytes du PDF à chaque affichage de tableau de candidatures.
    byte[] cvSnapshot;
    String cvSnapshotFileName;

    byte[] lettreMotivation;

    // --- Infos candidat figées au moment T (affichage rapide sans appel Feign vers user-service) ---
    String nomComplet;
    String email;
    String telephone;
    String specialite;
    String experience;
    String formation;

    @Builder.Default
    List<String> competences = new ArrayList<>(); // liste, pas texte brut — cohérent avec CvParsedData, exploitable pour le matching IA

    @Builder.Default
    List<String> langues = new ArrayList<>(); // manquait : nécessaire pour le futur languageMatch dans MlTrainingData

    @Indexed
    ApplicationStatus statut;

    EtatEntretien etatEntretien; // granularité fine du process d'entretien, complémentaire à statut

    LocalDate dateCandidature;
    LocalDateTime dateDernierChangementStatut;

    // --- Suivi RH ---
    String commentaireRH;

    Boolean entretienPlanifie;
    LocalDateTime dateEntretien;

    // --- Réservé au futur module IA de matching ---
    Double scoreMatching;
}