package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import service.recrutement.Entity.Enum.InterviewMode;
import service.recrutement.Entity.Enum.InterviewSource;
import service.recrutement.Entity.Enum.InterviewStatus;
import service.recrutement.Entity.Enum.InterviewType;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingInterviewDto {

    private String idInterview;

    private String applicationId;

    private String posteRecrutement;

    private String posteId;

    private InterviewType type;

    private InterviewSource source;

    private InterviewMode mode;

    private LocalDateTime dateEntretien;

    private LocalDateTime dateFinEntretien;

    private InterviewStatus statut;

    private String lieu;

    private String lienVisio;

    private String interviewerName;
}