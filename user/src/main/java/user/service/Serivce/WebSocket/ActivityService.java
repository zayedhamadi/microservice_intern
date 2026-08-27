package user.service.Serivce.WebSocket;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.service.Entity.Activity;
import user.service.Entity.Enum.ActivityType;
import user.service.Repository.ActivityRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ActivityService {

    ActivityRepository activityRepository;

    @Transactional
    public void deleteOldActivities(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        log.info("Suppression des activités anciennes avant le {}", cutoffDate);
        int deletedCount = activityRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("{} activités anciennes supprimées", deletedCount);
    }

    @Transactional
    public List<Map<String, Object>> getRecentAndCleanOld(int limit, int daysToKeep) {
        deleteOldActivities(daysToKeep);
        return getRecent(limit);
    }


    @Scheduled(cron = "0 0 0 * * ?")

    @Transactional
    public void scheduledDeleteOldActivities() {
        deleteOldActivities(30);

    }


    @Transactional
    public Activity log(ActivityType type, String actorPrenom, String actorNom, String role, String motif, String message) {
        if (message == null) {
            message = switch (type) {
                case NEW_USER -> "Nouvel utilisateur créé : " + actorPrenom + " " + actorNom;
                default -> "Action effectuée";
            };
        }

        Activity activity = Activity.builder()
                .type(type)
                .actorPrenom(actorPrenom)
                .actorNom(actorNom)
                .role(role)
                .motif(motif)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        return activityRepository.save(activity);
    }

    @Transactional
    public Activity save(String type, String message, String color, String role, String prenom, String nom) {
        ActivityType activityType;
        try {
            activityType = ActivityType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type d'activité inconnu : " + type, e);
        }
        return log(activityType, prenom, nom, role, null, message);
    }

    public List<Map<String, Object>> getRecent(int limit) {
        return activityRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(a -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("type", a.getType().name());
                    map.put("actorPrenom", a.getActorPrenom());
                    map.put("actorNom", a.getActorNom());
                    map.put("role", a.getRole());
                    map.put("motif", a.getMotif());
                    map.put("message", a.getMessage());
                    map.put("createdAt", a.getCreatedAt().toString());
                    return map;
                })
                .toList();
    }

    @Transactional
    public void clear() {
        activityRepository.deleteAllActivities();
    }
}