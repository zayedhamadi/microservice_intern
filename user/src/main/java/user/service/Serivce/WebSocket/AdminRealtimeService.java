package user.service.Serivce.WebSocket;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import user.service.Serivce.Admin.UserStatistics;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminRealtimeService {

    private static final String TOPIC_STATS = "/topic/admin.stats";
    private static final String TOPIC_EVENTS_ADMIN = "/topic/admin.events.admin";

    private static final String TOPIC_EVENTS_RH = "/topic/admin.events.rh";

    private static final String TOPIC_EVENTS_RH_EMPLOYEE = "/topic/admin.events.rh-employee";

    private static final String TOPIC_EVENTS_ALL = "/topic/admin.events.all";

    SimpMessagingTemplate messagingTemplate;
    UserStatistics userStatistics;
    ActivityService activityService;

    @Scheduled(fixedDelay = 120_000, initialDelay = 10_000)
    public void pushStats() {
        try {
            messagingTemplate.convertAndSend(TOPIC_STATS, AdminRealtimeEvent.statsUpdate(buildStatsPayload()));
            log.debug("Stats push → {}", TOPIC_STATS);
        } catch (Exception e) {
            log.error("Erreur pushStats : {}", e.getMessage());
        }
    }

    public void notifyNewUser(String prenom, String nom, String role) {
        try {
            activityService.save("NEW_USER", "Nouvel utilisateur " + role + " : " + prenom + " " + nom, "#1D9E75", role, prenom, nom);
            messagingTemplate.convertAndSend(TOPIC_EVENTS_RH_EMPLOYEE, AdminRealtimeEvent.newUser(prenom, nom, role));
            pushStats();
        } catch (Exception e) {
            log.error("Erreur notifyNewUser : {}", e.getMessage());
        }
    }

    public void notifyCessation(String prenom, String nom, String motif) {
        try {
            activityService.save("CESSATION", "Compte suspendu : " + prenom + " " + nom, "#ef4444", null, prenom, nom);
            messagingTemplate.convertAndSend(TOPIC_EVENTS_RH, AdminRealtimeEvent.cessation(prenom, nom, motif));
            pushStats();
        } catch (Exception e) {
            log.error("Erreur notifyCessation : {}", e.getMessage());
        }
    }

    public void notifyReactivation(String prenom, String nom) {
        try {
            activityService.save("REACTIVATION", "Compte réactivé : " + prenom + " " + nom, "#4a6cf7", null, prenom, nom);
            messagingTemplate.convertAndSend(TOPIC_EVENTS_RH, AdminRealtimeEvent.reactivation(prenom, nom));
            pushStats();
        } catch (Exception e) {
            log.error("Erreur notifyReactivation : {}", e.getMessage());
        }
    }

    public void notifyCertification(String prenom, String nom, String titre, String action) {
        try {
            activityService.save("CERTIFICATION", "Certification " + action + " — " + titre + " (" + prenom + " " + nom + ")", "AJOUT".equals(action) ? "#f59e0b" : "#ef4444", null, prenom, nom);
            messagingTemplate.convertAndSend(TOPIC_EVENTS_RH, AdminRealtimeEvent.certification(prenom, nom, titre, action));
        } catch (Exception e) {
            log.error("Erreur notifyCertification : {}", e.getMessage());
        }
    }

    public void notifyLoginActivity(Long userId, String prenom, String nom, String action, String email) {
        try {
            activityService.save("LOGIN_ACTIVITY", action + " : " + prenom + " " + nom, "LOGIN".equals(action) ? "#10b981" : "#6b7280", null, prenom, nom);
            messagingTemplate.convertAndSend(TOPIC_EVENTS_ADMIN, AdminRealtimeEvent.loginActivity(userId, prenom, nom, action, email));
        } catch (Exception e) {
            log.error("Erreur notifyLoginActivity : {}", e.getMessage());
        }
    }

    public void notifyLoginActivity(Long userId, String prenom, String nom, String action) {
        notifyLoginActivity(userId, prenom, nom, action, null);
    }

    public void notifyNewDepartement(String nom) {
        try {
            activityService.save("NEW_DEPARTEMENT", "Nouveau département : " + nom, "#0ea5e9", null, null, null);
            messagingTemplate.convertAndSend(TOPIC_EVENTS_ALL, AdminRealtimeEvent.createDepartement(nom));
        } catch (Exception e) {
            log.error("Erreur notifyNewDepartement : {}", e.getMessage());
        }
    }

//    public void notifyNewPoste(String nom) {
//        try {
//            activityService.save("NEW_POSTE", "Nouveau poste : " + nom, "#6366f1", null, null, null);
//            messagingTemplate.convertAndSend(TOPIC_EVENTS_ALL, AdminRealtimeEvent.createPoste(nom));
//        } catch (Exception e) {
//            log.error("Erreur notifyNewPoste : {}", e.getMessage());
//        }
//    }

    private Map<String, Object> buildStatsPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("users", userStatistics.getUsersStats());
        payload.put("rh", userStatistics.getRHStats());
        payload.put("employees", userStatistics.getEmployeeStats());
        payload.put("candidats", userStatistics.getCandidatsStats());
        payload.put("inactifs", userStatistics.getInactifsStats());
        payload.put("statusByRole", userStatistics.getStatusByRole());
        payload.put("genreByRole", userStatistics.getGenreByRole());
        payload.put("monthly", userStatistics.getMonthlyRegistrations());
        payload.put("inscrCess", userStatistics.getMonthlyInscrVsCessation());
        payload.put("last5", userStatistics.getLast5InscriptionUsers());
        return payload;
    }
}