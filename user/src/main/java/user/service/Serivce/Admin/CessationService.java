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
public class CessationService  {

    UserRepository userRepository;
    CessationRepository cessationRepository;
    UserEmailService userEmailService;
    AdminRealtimeService realtimeService;
    UserFinderService userFinderService;


    @Transactional
    public void cesserUser(Long userId, CessationDTO dto) {
        User user = this.userFinderService.byId(userId);

        Cessation cessation = Cessation.builder()
                .motifCessation(dto.getMotifCessation())
                .dateCessation(LocalDate.now())
                .build();

        cessation = cessationRepository.save(cessation);
        user.setCessation(cessation);
        user.setEtatCompte(Compte.INACTIF);
        userRepository.save(user);
//        try {
//            realtimeService.notifyCessation(user.getPrenom(), user.getNom(),
//                    dto.getMotifCessation());
//        } catch (Exception e) {
//            log.warn("WS notify cessation échoué : {}", e.getMessage());
//        }
        userEmailService.sendCessationEmail(
                user.getEmail(),
                user.getPrenom(),
                dto.getMotifCessation()
        );
        log.info("Compte {} cessé. Motif : {}", userId, dto.getMotifCessation());
    }

    @Transactional
    public void reactiverUser(Long userId) {
        User user = this.userFinderService.byId(userId);

        if (user.getCessation() != null) {
            Cessation c = user.getCessation();
            cessationRepository.save(c);
        }
//        try {
//        realtimeService.notifyReactivation(user.getPrenom(), user.getNom());
//    } catch (Exception e) {
//        log.warn("WS notify reactivation échoué : {}", e.getMessage());
//    }
        user.setEtatCompte(Compte.ACTIF);
        userRepository.save(user);
        userEmailService.sendReactivationEmail(
                user.getEmail(),
                user.getPrenom()
        );
        log.info("Compte {} réactivé. Motif : {}", userId);
    }
}