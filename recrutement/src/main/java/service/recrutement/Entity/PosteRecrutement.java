package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import service.recrutement.Entity.Enum.StatusPosteRecrutement;
import service.recrutement.Entity.Enum.TypeContrat;
import service.recrutement.Entity.Enum.WorkType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "postesRecrutement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PosteRecrutement {

    @Id
    String idPosteRecrutement;

    @Indexed
    String recruteurKeycloakId;

    String titre;
    String description;
    String profilDemandeOfPoste;

    @Builder.Default
    List<String> competencesRequises = new ArrayList<>();

    TypeContrat typeContrat;

    @Indexed
    StatusPosteRecrutement status;

    WorkType workType;

    String lieu;

    Long salaire;
    String devise; // "TND", "EUR"... ne pas figer une seule devise en dur

    @Builder.Default
    Integer nombrePostes = 1;

    LocalDate datePosteRecrutement;
    LocalDate dateExperationPosteRecrutement;

    LocalDateTime dateCreation;
    LocalDateTime dateModification;

    // Dérivé, plus stocké : évite la désynchro entre status et un booléen séparé
    @Transient
    public boolean isOuvert() {
        return status == StatusPosteRecrutement.OPEN
                && (dateExperationPosteRecrutement == null || !dateExperationPosteRecrutement.isBefore(LocalDate.now()));
    }
}