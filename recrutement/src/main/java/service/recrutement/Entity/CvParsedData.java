package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "cvParsedData")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CvParsedData {

    @Id
    String idCvParsedData;

    @Indexed(unique = true)
    String candidatKeycloakId;

    @Builder.Default
    List<String> competences = new ArrayList<>();

    @Builder.Default
    List<String> langues = new ArrayList<>();

    String formation;

    String experience;
    Integer anneesExperience;

    String specialite;
    String nomComplet;
    String email;
    String telephone;

    LocalDateTime parsedAt;
}