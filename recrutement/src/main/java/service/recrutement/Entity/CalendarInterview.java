package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import service.recrutement.Entity.Enum.CalendarInterviewMode;
import service.recrutement.Entity.Enum.CalendarInterviewStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entretien "libre" géré depuis le calendrier RH (composant Angular
 * calendrier-rh). Contrairement à {@link Interview}, il n'est PAS
 * forcément rattaché à une candidature : les infos candidat/poste/
 * recruteur sont saisies directement dans le formulaire.
 */
@Document(collection = "calendarInterviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CalendarInterview {

    @Id
    String id;

    String candidateName;
    String candidateEmail;

    String posteRecrutement;
    String posteId;

    String interviewerName;

    @Indexed
    LocalDate interviewDate;

    LocalTime startTime;
    LocalTime endTime;

    CalendarInterviewMode mode;

    String location;
    String meetingLink;

    @Indexed
    CalendarInterviewStatus status;

    String notes;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}