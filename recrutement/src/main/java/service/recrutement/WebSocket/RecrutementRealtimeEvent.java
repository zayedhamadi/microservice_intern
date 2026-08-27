package service.recrutement.WebSocket;

import lombok.*;
import lombok.experimental.FieldDefaults;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Interview;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.Reprogrammer;
import service.recrutement.Entity.Enum.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecrutementRealtimeEvent {

    RecrutementEventType type;
    Object payload;
    String timestamp;

    // ---- Postes ----

    public static RecrutementRealtimeEvent newPoste(PosteRecrutement poste) {
        return build(RecrutementEventType.NEW_POSTE, toPostePayload(poste, null));
    }

    public static RecrutementRealtimeEvent posteStatusChanged(PosteRecrutement poste, StatusPosteRecrutement ancienStatus) {
        return build(RecrutementEventType.POSTE_STATUS_CHANGED, toPostePayload(poste, ancienStatus));
    }

    // ---- Candidatures ----

    public static RecrutementRealtimeEvent newApplication(Application application, PosteRecrutement poste) {
        return build(RecrutementEventType.NEW_APPLICATION,
                toApplicationPayload(application, poste, null, application.getStatut()));
    }

    public static RecrutementRealtimeEvent applicationStatusChanged(Application application,
                                                                      ApplicationStatus ancien, ApplicationStatus nouveau) {
        return build(RecrutementEventType.APPLICATION_STATUS_CHANGED,
                toApplicationPayload(application, null, ancien, nouveau));
    }

    // ---- Entretiens ----

    public static RecrutementRealtimeEvent interviewPlanifie(Interview interview) {
        return build(RecrutementEventType.INTERVIEW_PLANIFIE, toInterviewPayload(interview));
    }

    public static RecrutementRealtimeEvent interviewReporte(Interview interview) {
        return build(RecrutementEventType.INTERVIEW_REPORTE, toInterviewPayload(interview));
    }

    public static RecrutementRealtimeEvent interviewAnnule(Interview interview) {
        return build(RecrutementEventType.INTERVIEW_ANNULE, toInterviewPayload(interview));
    }

    public static RecrutementRealtimeEvent interviewAbsent(Interview interview) {
        return build(RecrutementEventType.INTERVIEW_ABSENT, toInterviewPayload(interview));
    }

    public static RecrutementRealtimeEvent interviewResultat(Interview interview) {
        return build(RecrutementEventType.INTERVIEW_RESULTAT, toInterviewPayload(interview));
    }

    // ---- Reprogrammations ----

    public static RecrutementRealtimeEvent reprogrammationDemandee(Reprogrammer demande) {
        return build(RecrutementEventType.REPROGRAMMATION_DEMANDEE, toReprogrammationPayload(demande));
    }

    public static RecrutementRealtimeEvent reprogrammationTraitee(Reprogrammer demande) {
        return build(RecrutementEventType.REPROGRAMMATION_TRAITEE, toReprogrammationPayload(demande));
    }

    // ---- Construction des payloads (extraits des entités, pas d'exposition brute) ----

    private static PosteEventPayload toPostePayload(PosteRecrutement poste, StatusPosteRecrutement ancienStatus) {
        return new PosteEventPayload(
                poste.getIdPosteRecrutement(), poste.getTitre(), poste.getDepartementNom(),
                ancienStatus, poste.getStatus());
    }

    private static ApplicationEventPayload toApplicationPayload(Application application, PosteRecrutement poste,
                                                                 ApplicationStatus ancien, ApplicationStatus nouveau) {
        return new ApplicationEventPayload(
                application.getIdApplication(), application.getCandidatKeycloakId(), application.getNomComplet(),
                application.getPosteRecrutementId(), poste != null ? poste.getTitre() : null,
                ancien, nouveau, application.getCommentaireRH());
    }

    private static InterviewEventPayload toInterviewPayload(Interview interview) {
        return new InterviewEventPayload(
                interview.getIdInterview(), interview.getApplicationId(), interview.getCandidatKeycloakId(),
                interview.getCandidateName(), interview.getSource(), interview.getType(),
                interview.getDateEntretien(), interview.getMode(), interview.getLieu(), interview.getLienVisio(),
                interview.getStatut(), interview.getResultat(), interview.getNotes());
    }

    private static ReprogrammationEventPayload toReprogrammationPayload(Reprogrammer demande) {
        return new ReprogrammationEventPayload(
                demande.getIdReprogrammer(), demande.getInterviewId(), demande.getApplicationId(),
                demande.getType(), demande.getDemandeurKeycloakId(), demande.getCibleKeycloakId(),
                demande.getNouvelleDateProposee(), demande.getMotif(), demande.getStatut());
    }

    private static RecrutementRealtimeEvent build(RecrutementEventType type, Object payload) {
        return RecrutementRealtimeEvent.builder().type(type).payload(payload).timestamp(now()).build();
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // ---- Payloads ----

    @Getter @AllArgsConstructor
    public static class PosteEventPayload {
        String idPoste;
        String titre;
        String departementNom;
        StatusPosteRecrutement ancienStatus;
        StatusPosteRecrutement nouveauStatus;
    }

    @Getter @AllArgsConstructor
    public static class ApplicationEventPayload {
        String applicationId;
        String candidatKeycloakId;
        String candidateName;
        String posteId;
        String posteTitre;
        ApplicationStatus ancienStatut;
        ApplicationStatus nouveauStatut;
        String commentaireRH;
    }

    @Getter @AllArgsConstructor
    public static class InterviewEventPayload {
        String interviewId;
        String applicationId;
        String candidatKeycloakId;
        String candidateName;
        InterviewSource source;
        InterviewType type;
        LocalDateTime dateEntretien;
        InterviewMode mode;
        String lieu;
        String lienVisio;
        InterviewStatus statut;
        InterviewResult resultat;
        String notes;
    }

    @Getter @AllArgsConstructor
    public static class ReprogrammationEventPayload {
        String reprogrammerId;
        String interviewId;
        String applicationId;
        TypeDemandeReport type;
        String demandeurKeycloakId;
        String cibleKeycloakId;
        LocalDateTime nouvelleDateProposee;
        String motif;
        DemandeReportStatus statut;
    }
}