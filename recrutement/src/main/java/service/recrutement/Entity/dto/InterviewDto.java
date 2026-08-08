package service.recrutement.Entity.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import service.recrutement.Entity.Enum.InterviewMode;
import service.recrutement.Entity.Enum.InterviewResult;
import service.recrutement.Entity.Enum.InterviewStatus;
import service.recrutement.Entity.Enum.InterviewType;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDto {
    private String idInterview;
    private String applicationId;
    private InterviewType type;
    private LocalDateTime dateEntretien;
    private String lieu;
    private String lienVisio;
    private String notes;
    private InterviewStatus statut;
    private InterviewResult resultat;
    private LocalDateTime dateCreation;
    private InterviewMode mode;
    private LocalDateTime dateModification;
}