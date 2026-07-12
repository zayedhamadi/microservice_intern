// Serivce/Admin/EmployeeManagement.java — mis à jour avec log d'activité
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
          if (dto.getRole() == Role.CANDIDAT) {
               throw new RuntimeException("Le rôle candidat ne peut pas être choisi à l'inscription.");
          }
          if (userRepository.existsByEmail(dto.getEmail())) {
               throw new RuntimeException("Email déjà utilisé : " + dto.getEmail());
          }

          String password = generatePw();
          dto.setPassword(password);
          String matricule = generateMatricule();
          dto.setMatricule(matricule);

          String keycloakId = keycloakService.createUserInKeycloak(dto);
          log.info("Keycloak ID obtenu : {}", keycloakId);

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
          log.info("User sauvegardé en DB, ID : {}", saved.getId());

          try {
               realtimeService.notifyNewUser(saved.getPrenom(), saved.getNom(), saved.getRole().name());
          } catch (Exception e) {
               log.warn("WS notify newUser échoué : {}", e.getMessage());
          }

          try {
               activityService.log(ActivityType.NEW_USER, saved.getPrenom(), saved.getNom(),
                       saved.getRole().name(), null, null);
          } catch (Exception e) {
               log.warn("Log activité newUser échoué : {}", e.getMessage());
          }

          userEmailService.sendAccountCreationEmail(
                  dto.getEmail(), dto.getPrenom(), dto.getNom(), matricule, password, "http://localhost:4200/login"
          );

          return saved;
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