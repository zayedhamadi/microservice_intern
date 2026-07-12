package user.service.Serivce.WebSocket;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.service.Entity.Activity;
import user.service.Entity.Enum.ActivityType;
import user.service.Repository.ActivityRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ActivityService {

    ActivityRepository activityRepository;

    @Transactional
    public Activity log(ActivityType type, String prenom, String nom, String role, String motif, String message) {
        Activity activity = Activity.builder()
                .type(type)
                .actorPrenom(prenom)
                .actorNom(nom)
                .role(role)
                .motif(motif)
                .message(message)
                .build();
        return activityRepository.save(activity);
    }

    /**
     * Point d'entrée utilisé par AdminRealtimeService.
     * Signature : (type, message, color, role, prenom, nom).
     * "color" n'est pas persistée (elle est recalculée côté front via eventColor(type)),
     * on la reçoit juste pour ne pas casser les appels existants.
     */
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
        return activityRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", a.getType().name());
                    m.put("actorPrenom", a.getActorPrenom());
                    m.put("actorNom", a.getActorNom());
                    m.put("role", a.getRole());
                    m.put("motif", a.getMotif());
                    m.put("message", a.getMessage());
                    m.put("createdAt", a.getCreatedAt().toString());
                    return m;
                })
                .toList();
    }

    @Transactional
    public void clear() {
        activityRepository.deleteAllActivities();
    }
}