package service.recrutement.Service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class GoogleMeetService {

    private static final String TIME_ZONE = "Africa/Tunis";

    private Calendar calendarClient;

    private String calendarId;

    private boolean configured = false;

    public GoogleMeetService(
            @Value("${google.calendar.client-id:}") String clientId,
            @Value("${google.calendar.client-secret:}") String clientSecret,
            @Value("${google.calendar.refresh-token:}") String refreshToken,
            @Value("${google.calendar.calendar-id:primary}") String calendarId) {

        /*
         * Vérification de la configuration Google OAuth
         */
        if (clientId == null || clientId.isBlank()
                || clientSecret == null || clientSecret.isBlank()
                || refreshToken == null || refreshToken.isBlank()) {

            log.warn(
                    "GoogleMeetService : configuration OAuth Google Calendar absente. "
                            + "La génération des liens Google Meet est désactivée."
            );

            return;
        }

        try {

            this.calendarId = calendarId;

            /*
             * =========================================================
             * OAuth 2.0 utilisateur
             * =========================================================
             *
             * Le refresh token a été obtenu lors de l'autorisation
             * initiale du compte Google.
             *
             * UserCredentials renouvelle automatiquement l'access token
             * lorsque celui-ci expire.
             */
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .build();

            /*
             * =========================================================
             * Google Calendar API client
             * =========================================================
             */
            this.calendarClient = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName("recrutement-service")
                    .build();

            this.configured = true;

            log.info(
                    "GoogleMeetService initialisé avec succès. Calendar ID: {}",
                    this.calendarId
            );

        } catch (Exception e) {

            log.error(
                    "GoogleMeetService : échec de l'initialisation. "
                            + "La génération de liens Google Meet est désactivée.",
                    e
            );
        }
    }

    /**
     * Crée un événement Google Calendar et génère
     * automatiquement une conférence Google Meet.
     *
     * @param titre titre de l'entretien
     * @param candidateEmail email du candidat
     * @param interviewerEmail email du recruteur
     * @param dateEntretien date et heure de début
     * @return lien Google Meet ou null en cas d'erreur
     */
    public String genererLienMeet(
            String titre,
            String candidateEmail,
            String interviewerEmail,
            LocalDateTime dateEntretien) {

        /*
         * =========================================================
         * Vérification de la configuration
         * =========================================================
         */
        if (!configured) {

            log.warn(
                    "Génération de lien Google Meet ignorée : "
                            + "Google Calendar n'est pas configuré."
            );

            return null;
        }

        try {

            /*
             * =========================================================
             * 1. Création de l'événement Google Calendar
             * =========================================================
             *
             * Durée de l'entretien : 1 heure.
             */
            Event event = new Event()
                    .setSummary(titre)
                    .setDescription(
                            "Entretien de recrutement "
                                    + "organisé via la plateforme de recrutement."
                    )
                    .setStart(
                            toEventDateTime(dateEntretien)
                    )
                    .setEnd(
                            toEventDateTime(
                                    dateEntretien.plusHours(1)
                            )
                    );

            /*
             * =========================================================
             * 2. Ajout des participants
             * =========================================================
             */
            List<EventAttendee> attendees = new ArrayList<>();

            if (candidateEmail != null && !candidateEmail.isBlank()) {

                attendees.add(
                        new EventAttendee()
                                .setEmail(candidateEmail.trim())
                );
            }

            if (interviewerEmail != null && !interviewerEmail.isBlank()) {

                attendees.add(
                        new EventAttendee()
                                .setEmail(interviewerEmail.trim())
                );
            }

            if (!attendees.isEmpty()) {
                event.setAttendees(attendees);
            }

            /*
             * =========================================================
             * 3. Demande de création d'une conférence Google Meet
             * =========================================================
             */
            ConferenceData conferenceData = new ConferenceData()
                    .setCreateRequest(
                            new CreateConferenceRequest()
                                    .setRequestId(
                                            UUID.randomUUID().toString()
                                    )
                                    .setConferenceSolutionKey(
                                            new ConferenceSolutionKey()
                                                    .setType("hangoutsMeet")
                                    )
                    );

            event.setConferenceData(conferenceData);

            /*
             * =========================================================
             * 4. Création de l'événement dans Google Calendar
             * =========================================================
             */
            Event createdEvent = calendarClient
                    .events()
                    .insert(calendarId, event)
                    .setConferenceDataVersion(1)
                    .setSendUpdates("all")
                    .execute();

            /*
             * =========================================================
             * 5. Récupération du lien Google Meet
             * =========================================================
             */
            String meetLink = createdEvent.getHangoutLink();

            if (meetLink == null || meetLink.isBlank()) {

                log.error(
                        "Google Calendar a créé l'événement, "
                                + "mais aucun lien Google Meet n'a été retourné."
                );

                throw new IllegalStateException(
                        "Google n'a pas renvoyé de lien Google Meet."
                );
            }

            /*
             * =========================================================
             * 6. Logs
             * =========================================================
             */
            log.info(
                    "Événement Google Calendar créé avec succès. Event ID: {}",
                    createdEvent.getId()
            );

            log.info(
                    "Lien Google Meet généré : {}",
                    meetLink
            );

            return meetLink;

        } catch (Exception e) {

            log.error(
                    "Erreur lors de la création de l'événement "
                            + "Google Calendar / Google Meet.",
                    e
            );

            return null;
        }
    }

    /**
     * Convertit un LocalDateTime vers le format attendu
     * par Google Calendar.
     *
     * Fuseau horaire : Africa/Tunis
     */
    private EventDateTime toEventDateTime(LocalDateTime dateTime) {

        return new EventDateTime()
                .setDateTime(
                        new com.google.api.client.util.DateTime(
                                dateTime
                                        .atZone(
                                                ZoneId.of(TIME_ZONE)
                                        )
                                        .toInstant()
                                        .toEpochMilli()
                        )
                )
                .setTimeZone(TIME_ZONE);
    }
}