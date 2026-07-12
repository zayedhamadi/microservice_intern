package user.service.Serivce.Admin;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.service.Dto.createUserPerAdminDto;
import user.service.Entity.Enum.Compte;
import user.service.Entity.Enum.Role;
import user.service.Entity.User;
import user.service.Mail.UserEmailService;
import user.service.Repository.UserRepository;
import user.service.Serivce.Authorization.KeycloakService;
import user.service.Serivce.UserCommun.UserFinderService;

import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeManagement {
     UserRepository userRepository;
     UserFinderService userFinderService;
     UserEmailService userEmailService;
     KeycloakService keycloakService;

     @Transactional
     public User register(createUserPerAdminDto dto) {
          if (dto.getRole() == Role.CANDIDAT) {
               throw new RuntimeException("Le rôle candidat ne peut pas être choisi à l'inscription.");
          }

          if (userRepository.existsByEmail(dto.getEmail())) {
               throw new RuntimeException("Email déjà utilisé : " + dto.getEmail());
          }

          // Générer un mot de passe aléatoire
          String password = generatePw();
          dto.setPassword(password);

          // Générer un matricule
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

          // Envoyer un email avec toutes les informations (identifiants + matricule)
          userEmailService.sendAccountCreationEmail(
                  dto.getEmail(),
                  dto.getPrenom(),
                  dto.getNom(),
                  matricule,
                  password,
                  "http://localhost:4200/login"
          );

          return saved;
     }

     public String generatePw() {
          // Générer un mot de passe aléatoire de 10 caractères
          String uuid = UUID.randomUUID().toString().replace("-", "");
          return uuid.substring(0, 10);
     }

     public String generateMatricule() {
          // Générer un matricule unique (ex: EMP-2026-001)
          Random random = new Random();
          int randomNum = 100 + random.nextInt(900); // Nombre entre 100 et 999
          return "EMP-" + java.time.Year.now() + "-" + randomNum;
     }
}