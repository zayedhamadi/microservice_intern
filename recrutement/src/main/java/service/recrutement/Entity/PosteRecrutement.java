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

    @Builder.Default
    List<String> languesRequises = new ArrayList<>();

    Integer anneesExperienceMin;

    String niveauEtudeRequis;

    TypeContrat typeContrat;

    @Indexed
    StatusPosteRecrutement status;

    WorkType workType;

    String lieu;

    Long salaire;

    @Builder.Default
    Integer nombrePostes = 1;


    @Indexed
    String departementNom;

    LocalDate datePosteRecrutement;
    LocalDate dateExpirationPosteRecrutement;

    LocalDateTime dateCreation;
    LocalDateTime dateModification;

    @Transient
    public boolean isOuvert() {
        return status == StatusPosteRecrutement.OUVERT
                && (dateExpirationPosteRecrutement == null || !dateExpirationPosteRecrutement.isBefore(LocalDate.now()));
    }
}