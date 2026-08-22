package service.recrutement.Entity.dto;

import lombok.*;
import service.recrutement.Entity.Enum.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidatEntretienTechniqueDto {
    private String interviewId;
    private String applicationId;
    private String candidatKeycloakId;
    private String candidateName;
    private String candidateEmail;
    private String interviewDate;
    private String startTime;
    private String endTime;
    private InterviewMode mode;
    private String location;
    private String meetingLink;
    private InterviewStatus status;
    private InterviewResult resultat;
    private String notes;
    private String interviewerName;
}