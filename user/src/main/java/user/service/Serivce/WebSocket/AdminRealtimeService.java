package user.service.Serivce.WebSocket;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import user.service.Dto.AdminRealtimeEvent;
import user.service.Serivce.Admin.UserStatistics;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminRealtimeService {


    SimpMessagingTemplate messagingTemplate;
    UserStatistics        userStatistics;
    ActivityService       activityService;

    static final String TOPIC_STATS  = "/topic/admin.stats";
    static final String TOPIC_EVENTS = "/topic/admin.events";

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
            activityService.save("NEW_USER", "Nouvel utilisateur " + role + " : " + prenom + " " + nom,
                    "#1D9E75", role, prenom, nom);
            messagingTemplate.convertAndSend(TOPIC_EVENTS, AdminRealtimeEvent.newUser(prenom, nom, role));
            pushStats();
        } catch (Exception e) {
            log.error("Erreur notifyNewUser : {}", e.getMessage());
        }
    }

    public void notifyCessation(String prenom, String nom, String motif) {
        try {
            activityService.save("CESSATION", "Compte suspendu : " + prenom + " " + nom, "#ef4444", null, prenom, nom);
            messagingTemplate.convertAndSend(TOPIC_EVENTS, AdminRealtimeEvent.cessation(prenom, nom, motif));
            pushStats();
        } catch (Exception e) {
            log.error("Erreur notifyCessation : {}", e.getMessage());
        }
    }

    public void notifyReactivation(String prenom, String nom) {
        try {
            activityService.save("REACTIVATION", "Compte réactivé : " + prenom + " " + nom, "#4a6cf7", null, prenom, nom);
            messagingTemplate.convertAndSend(TOPIC_EVENTS, AdminRealtimeEvent.reactivation(prenom, nom));
            pushStats();
        } catch (Exception e) {
            log.error("Erreur notifyReactivation : {}", e.getMessage());
        }
    }

    public void notifyLoginActivity(Long userId, String prenom, String nom, String action, String email) {
        try {
            activityService.save("LOGIN_ACTIVITY", action + " : " + prenom + " " + nom,
                    "LOGIN".equals(action) ? "#10b981" : "#6b7280", null, prenom, nom);
            messagingTemplate.convertAndSend(TOPIC_EVENTS,
                    AdminRealtimeEvent.loginActivity(userId, prenom, nom, action, email));
        } catch (Exception e) {
            log.error("Erreur notifyLoginActivity : {}", e.getMessage());
        }
    }

    public void notifyLoginActivity(Long userId, String prenom, String nom, String action) {
        notifyLoginActivity(userId, prenom, nom, action, null);
    }

    public void notifyCertification(String prenom, String nom, String titre, String action) {
        try {
            activityService.save("CERTIFICATION",
                    "Certification " + action + " — " + titre + " (" + prenom + " " + nom + ")",
                    "AJOUT".equals(action) ? "#f59e0b" : "#ef4444", null, prenom, nom);
            messagingTemplate.convertAndSend(TOPIC_EVENTS, AdminRealtimeEvent.certification(prenom, nom, titre, action));
        } catch (Exception e) {
            log.error("Erreur notifyCertification : {}", e.getMessage());
        }
    }

    private Map<String, Object> buildStatsPayload() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("users",        userStatistics.getUsersStats());
        p.put("rh",           userStatistics.getRHStats());
        p.put("employees",    userStatistics.getEmployeeStats());
        p.put("candidats",    userStatistics.getCandidatsStats());
        p.put("inactifs",     userStatistics.getInactifsStats());
        p.put("statusByRole", userStatistics.getStatusByRole());
        p.put("genreByRole",  userStatistics.getGenreByRole());
        p.put("monthly",      userStatistics.getMonthlyRegistrations());
        p.put("inscrCess",    userStatistics.getMonthlyInscrVsCessation());
        p.put("last5",        userStatistics.getLast5InscriptionUsers());
        return p;
    }
}