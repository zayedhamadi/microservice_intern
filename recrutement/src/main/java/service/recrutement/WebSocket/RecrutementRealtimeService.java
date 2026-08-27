package service.recrutement.WebSocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Interview;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.Reprogrammer;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.Enum.StatusPosteRecrutement;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecrutementRealtimeService {

    private static final String TOPIC_RH = "/topic/recrutement.rh";
    private static final String TOPIC_EMPLOYEE = "/topic/recrutement.employee";
    private static final String TOPIC_ALL = "/topic/recrutement.all";
    // Canal personnel : utilisable par N'IMPORTE QUEL utilisateur (RH, EMPLOYEE ou CANDIDAT),
    // chacun s'abonne à son propre keycloakId au login, quel que soit son rôle.
    private static final String TOPIC_USER_PREFIX = "/topic/recrutement.user.";

    private final SimpMessagingTemplate messagingTemplate;

    private String userTopic(String keycloakId) {
        return TOPIC_USER_PREFIX + keycloakId;
    }

    // ==================== Postes ====================

    public void notifyNewPoste(PosteRecrutement poste) {
        send(TOPIC_ALL, RecrutementRealtimeEvent.newPoste(poste));
    }

    public void notifyPosteStatusChanged(PosteRecrutement poste, StatusPosteRecrutement ancienStatus) {
        RecrutementRealtimeEvent event = RecrutementRealtimeEvent.posteStatusChanged(poste, ancienStatus);
        send(TOPIC_ALL, event);
        send(TOPIC_RH, event);
    }

    // ==================== Candidatures ====================

    public void notifyNewApplication(Application application, PosteRecrutement poste) {
        send(TOPIC_RH, RecrutementRealtimeEvent.newApplication(application, poste));
    }

    public void notifyApplicationStatusChanged(Application application, ApplicationStatus ancien, ApplicationStatus nouveau) {
        RecrutementRealtimeEvent event = RecrutementRealtimeEvent.applicationStatusChanged(application, ancien, nouveau);
        sendToUserSiPresent(application.getCandidatKeycloakId(), event);
        send(TOPIC_RH, event);
    }

    // ==================== Entretiens ====================

    public void notifyInterviewPlanifie(Interview interview) {
        broadcastInterviewEvent(RecrutementRealtimeEvent.interviewPlanifie(interview), interview);
    }

    public void notifyInterviewReporte(Interview interview) {
        broadcastInterviewEvent(RecrutementRealtimeEvent.interviewReporte(interview), interview);
    }

    public void notifyInterviewAnnule(Interview interview) {
        broadcastInterviewEvent(RecrutementRealtimeEvent.interviewAnnule(interview), interview);
    }

    public void notifyInterviewAbsent(Interview interview) {
        broadcastInterviewEvent(RecrutementRealtimeEvent.interviewAbsent(interview), interview);
    }

    public void notifyInterviewResultat(Interview interview) {
        broadcastInterviewEvent(RecrutementRealtimeEvent.interviewResultat(interview), interview);
    }

    private void broadcastInterviewEvent(RecrutementRealtimeEvent event, Interview interview) {
        // Le candidat voit l'évolution de SON entretien (peut être null pour un entretien LIBRE)
        sendToUserSiPresent(interview.getCandidatKeycloakId(), event);
        // L'audience métier : RH pour RH_INITIAL/RH_FINAL/LIBRE (type null), EMPLOYEE pour TECHNIQUE
        send(audienceTopicFor(interview.getType()), event);
    }

    private String audienceTopicFor(InterviewType type) {
        return type == InterviewType.TECHNIQUE ? TOPIC_EMPLOYEE : TOPIC_RH;
    }

    // ==================== Reprogrammations ====================

    public void notifyReprogrammationDemandee(Reprogrammer demande) {
        RecrutementRealtimeEvent event = RecrutementRealtimeEvent.reprogrammationDemandee(demande);
        // La cible peut être un candidat (proposition d'un intervenant) OU un RH/EMPLOYEE assigné
        // (demande du candidat) OU personne (réactivation après absence -> RH générique).
        sendToUserSiPresent(demande.getCibleKeycloakId(), event);
        send(TOPIC_RH, event);
    }

    public void notifyReprogrammationTraitee(Reprogrammer demande) {
        RecrutementRealtimeEvent event = RecrutementRealtimeEvent.reprogrammationTraitee(demande);
        sendToUserSiPresent(demande.getDemandeurKeycloakId(), event);
        send(TOPIC_RH, event);
    }

    // ==================== Utilitaires ====================

    private void sendToUserSiPresent(String keycloakId, RecrutementRealtimeEvent event) {
        if (keycloakId != null && !keycloakId.isBlank()) {
            send(userTopic(keycloakId), event);
        }
    }

    private void send(String topic, RecrutementRealtimeEvent event) {
        try {
            messagingTemplate.convertAndSend(topic, event);
            log.debug("Recrutement event {} -> {}", event.getType(), topic);
        } catch (Exception e) {
            log.error("Erreur envoi WebSocket recrutement sur {} : {}", topic, e.getMessage());
        }
    }
}