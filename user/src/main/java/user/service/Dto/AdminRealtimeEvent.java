package user.service.Dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminRealtimeEvent {

    EventType type;
    Object    payload;
    String    timestamp;

    public static AdminRealtimeEvent statsUpdate(Object statsPayload) {
        return AdminRealtimeEvent.builder().type(EventType.STATS_UPDATE).payload(statsPayload).timestamp(now()).build();
    }

    public static AdminRealtimeEvent newUser(String prenom, String nom, String role) {
        return AdminRealtimeEvent.builder().type(EventType.NEW_USER)
                .payload(new UserEventPayload(prenom, nom, role, "INSCRIPTION")).timestamp(now()).build();
    }

    public static AdminRealtimeEvent cessation(String prenom, String nom, String motif) {
        return AdminRealtimeEvent.builder().type(EventType.CESSATION)
                .payload(new UserEventPayload(prenom, nom, null, motif)).timestamp(now()).build();
    }

    public static AdminRealtimeEvent reactivation(String prenom, String nom) {
        return AdminRealtimeEvent.builder().type(EventType.REACTIVATION)
                .payload(new UserEventPayload(prenom, nom, null, "RÉACTIVATION")).timestamp(now()).build();
    }

    public static AdminRealtimeEvent loginActivity(Long userId, String prenom, String nom, String action) {
        return loginActivity(userId, prenom, nom, action, null);
    }

    public static AdminRealtimeEvent loginActivity(Long userId, String prenom, String nom, String action, String email) {
        return AdminRealtimeEvent.builder().type(EventType.LOGIN_ACTIVITY)
                .payload(new LoginEventPayload(userId, prenom, nom, action, email, LocalDateTime.now()))
                .timestamp(now()).build();
    }

    public static AdminRealtimeEvent certification(String prenom, String nom, String titre, String action) {
        return AdminRealtimeEvent.builder().type(EventType.CERTIFICATION)
                .payload(new CertificationEventPayload(prenom, nom, titre, action)).timestamp(now()).build();
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public enum EventType { STATS_UPDATE, NEW_USER, CESSATION, REACTIVATION, LOGIN_ACTIVITY, CERTIFICATION }

    @Getter
    @AllArgsConstructor
    public static class UserEventPayload {
        String prenom, nom, role, action;
    }

    @Getter
    @AllArgsConstructor
    public static class CertificationEventPayload {
        String prenom, nom, titre, action;
    }
}