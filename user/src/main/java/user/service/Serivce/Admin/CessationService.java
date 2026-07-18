package user.service.Serivce.Admin;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.service.Dto.CessationDTO;
import user.service.Entity.Cessation;
import user.service.Entity.Enum.Compte;
import user.service.Entity.User;
import user.service.Mail.UserEmailService;
import user.service.Repository.CessationRepository;
import user.service.Repository.UserRepository;
import user.service.Serivce.UserCommun.UserFinderService;
import user.service.Serivce.WebSocket.AdminRealtimeService;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CessationService {

    UserRepository userRepository;
    CessationRepository cessationRepository;
    UserEmailService userEmailService;
    AdminRealtimeService realtimeService;
    UserFinderService userFinderService;


    public void notifyAndEmail(Long userId, String motif) {
        User user = userFinderService.byId(userId);
        try {
            realtimeService.notifyCessation(user.getPrenom(), user.getNom(), motif);
        } catch (Exception e) {
            log.warn("WS notify cessation échoué : {}", e.getMessage());
        }
        userEmailService.sendCessationEmail(user.getEmail(), user.getPrenom(), motif);
    }

    @Transactional
    public void cesserUser(Long userId, CessationDTO dto) {
        User user = this.userFinderService.byId(userId);

        if (user.getEtatCompte() == Compte.INACTIF) {
            throw new RuntimeException("Ce compte est déjà inactif.");
        }

        Cessation cessation = Cessation.builder()
                .motifCessation(dto.getMotifCessation())
                .dateCessation(LocalDate.now())
                .build();

        cessation = cessationRepository.saveAndFlush(cessation); // ← flush immédiat

        user.setCessation(cessation);
        user.setEtatCompte(Compte.INACTIF);
        userRepository.save(user);

        log.info("Compte {} cessé. Motif : {}", userId, dto.getMotifCessation());
    }

    @Transactional
    public void reactiverUser(Long userId) {
        User user = this.userFinderService.byId(userId);

        if (user.getEtatCompte() == Compte.ACTIF) {
            throw new RuntimeException("Ce compte est déjà actif.");
        }



        user.setEtatCompte(Compte.ACTIF);
        userRepository.save(user);

        try {
            realtimeService.notifyReactivation(user.getPrenom(), user.getNom());
        } catch (Exception e) {
            log.warn("WS notify reactivation échoué : {}", e.getMessage());
        }

        userEmailService.sendReactivationEmail(user.getEmail(), user.getPrenom());
        log.info("Compte {} réactivé.", userId);
    }
}