package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import service.recrutement.Entity.Enum.InterviewResult;
import service.recrutement.Entity.Enum.InterviewStatus;
import service.recrutement.Entity.Enum.InterviewType;

import java.time.LocalDateTime;

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
    String applicationId;

    @Indexed
    String candidatKeycloakId;

    String recruteurKeycloakId;

    InterviewType type;

    LocalDateTime dateEntretien;

    String lieu;
    String lienVisio;

    String notes;

    InterviewStatus statut;
    InterviewResult resultat;

    LocalDateTime dateCreation;
    LocalDateTime dateModification;
}