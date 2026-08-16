package service.recrutement.Entity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import service.recrutement.Entity.Enum.CalendarInterviewMode;
import service.recrutement.Entity.Enum.CalendarInterviewStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CalendarInterviewDto {

    private String id;

    @NotBlank(message = "Le nom du candidat est requis")
    private String candidateName;

    @Email(message = "Email invalide")
    private String candidateEmail;

    @NotBlank(message = "Le poste est requis")
    private String posteRecrutement;

    private String posteId;

    @NotBlank(message = "L'interviewer est requis")
    private String interviewerName;

    @NotNull(message = "La date de l'entretien est requise")
    private String interviewDate; // "yyyy-MM-dd"

    @NotNull(message = "L'heure de début est requise")
    private String startTime; // "HH:mm"

    @NotNull(message = "L'heure de fin est requise")
    private String endTime; // "HH:mm"

    @NotNull(message = "Le mode est requis")
    private CalendarInterviewMode mode;

    private String location;
    private String meetingLink;

    @NotNull(message = "Le statut est requis")
    private CalendarInterviewStatus status;

    private String notes;

    private String createdAt;
    private String updatedAt;
}