package user.service.Serivce.Admin;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.service.Dto.createUserPerAdminDto;
import user.service.Entity.Enum.ActivityType;
import user.service.Entity.Enum.Compte;
import user.service.Entity.Enum.Role;
import user.service.Entity.User;
import user.service.Mail.UserEmailService;
import user.service.Repository.UserRepository;
import user.service.Serivce.Authorization.KeycloakService;
import user.service.Serivce.WebSocket.ActivityService;
import user.service.Serivce.WebSocket.AdminRealtimeService;

import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeManagement {

    UserRepository userRepository;
    UserEmailService userEmailService;
    KeycloakService keycloakService;
    AdminRealtimeService realtimeService;
    ActivityService activityService;


    @Transactional
    public User register(createUserPerAdminDto dto) {
        log.info("1. Début register() pour email : {}", dto.getEmail());
        if (dto.getRole() == Role.CANDIDAT) {
            throw new RuntimeException("Rôle candidat interdit.");
        }
        log.info("2. Rôle validé : {}", dto.getRole());

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email existe : " + dto.getEmail());
        }
        log.info("3. Email unique validé.");

        String password = generatePw();
        dto.setPassword(password);
        String matricule = generateMatricule();
        dto.setMatricule(matricule);
        log.info("4. Mot de passe et matricule générés.");

        String keycloakId = keycloakService.createUserInKeycloak(dto);
        log.info("5. Keycloak ID : {}", keycloakId);

        User user = User.builder()
                .keycloakId(keycloakId)
                .email(dto.getEmail())
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .role(dto.getRole())
                .matricule(matricule)
                .etatCompte(Compte.ACTIF)
                .build();

        User saved = userRepository.save(user);
        log.info("6. User sauvegardé en DB avec ID : {}", saved.getId());

        return saved; // <-- Fin de la transaction ici
    }

    // Méthode séparée (hors transaction)
    public void postRegister(User user, createUserPerAdminDto dto) {
        try {
            realtimeService.notifyNewUser(user.getPrenom(), user.getNom(), user.getRole().name());
            log.info("7. Notification WebSocket envoyée.");
        } catch (Exception e) {
            log.warn("7. Échec WebSocket : {}", e.getMessage());
        }

        try {
            // Message non null
            activityService.log(
                    ActivityType.NEW_USER,
                    user.getPrenom(),
                    user.getNom(),
                    user.getRole().name(),
                    null,
                    "Nouvel utilisateur créé : " + user.getPrenom() + " " + user.getNom()
            );
            log.info("8. Log activité ajouté.");
        } catch (Exception e) {
            log.warn("8. Échec log activité : {}", e.getMessage());
        }

        try {
            userEmailService.sendAccountCreationEmail(
                    dto.getEmail(), dto.getPrenom(), dto.getNom(), user.getMatricule(), dto.getPassword(),
                    "http://localhost:4200/login"
            );
            log.info("9. Email envoyé.");
        } catch (Exception e) {
            log.error("9. Échec envoi email : {}", e.getMessage());
        }
    }

    public String generatePw() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid.substring(0, 10);
    }

    public String generateMatricule() {
        Random random = new Random();
        int randomNum = 100 + random.nextInt(900);
        return "EMP-" + java.time.Year.now() + "-" + randomNum;
    }
}