package user.service.Serivce.UserCommun;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.service.Dto.CertificationDTO;
import user.service.Entity.Certification;
import user.service.Entity.User;
import user.service.Repository.*;
import user.service.Serivce.FileService;


import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificationService {

    CertificationRepository certificationRepository;
    UserRepository userRepository;
    FileService fileService;
    //AdminRealtimeService realtimeService;


    public List<CertificationDTO> getMyCertifications(Long userId) {
        return certificationRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }


    public CertificationDTO getCertificationById(Long certifId, Long userId) {
        Certification certif = findCertification(certifId);
        assertOwner(certif, userId);
        return toDTO(certif);
    }


    @Transactional
    public CertificationDTO addCertification(Long userId, CertificationDTO dto) {
        User user = findUser(userId);

        byte[] pdfBytes = fileService.decodeBase64(dto.getPdfBase64());

        Certification certif = Certification.builder()
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .pdfCertif(pdfBytes)
                .user(user)
                .build();

        Certification saved = certificationRepository.save(certif);
        log.info("Certification ajoutée : {} pour user id={}", saved.getTitre(), userId);

        //notifyCertificationEvent(user, saved.getTitre(), "AJOUT");
        return toDTO(saved);
    }


    @Transactional
    public CertificationDTO updateCertification(Long certifId, Long userId, CertificationDTO dto) {
        Certification certif = findCertification(certifId);
        assertOwner(certif, userId);

        if (dto.getTitre() != null) certif.setTitre(dto.getTitre());
        if (dto.getDescription() != null) certif.setDescription(dto.getDescription());

        byte[] pdfBytes = fileService.decodeBase64(dto.getPdfBase64());
        if (pdfBytes != null) certif.setPdfCertif(pdfBytes);

        Certification updated = certificationRepository.save(certif);
        log.info("Certification mise à jour : {}", certifId);

        //notifyCertificationEvent(updated.getUser(), updated.getTitre(), "MODIFICATION");
        return toDTO(updated);
    }


    @Transactional
    public void deleteCertification(Long certifId, Long userId) {
        Certification certif = findCertification(certifId);
        assertOwner(certif, userId);
        certificationRepository.delete(certif);
        log.info("Certification supprimée : {}", certifId);

        //notifyCertificationEvent(user, titre, "SUPPRESSION");
    }





    public List<CertificationDTO> getAllCertifications() {
        return certificationRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    public List<CertificationDTO> getCertificationsByUserId(Long userId) {
        findUser(userId);
        return certificationRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }


    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + userId));
    }

    private Certification findCertification(Long certifId) {
        return certificationRepository.findById(certifId)
                .orElseThrow(() -> new RuntimeException("Certification introuvable : " + certifId));
    }

    private void assertOwner(Certification certif, Long userId) {
        if (!certif.getUser().getId().equals(userId)) {
            throw new RuntimeException("Accès refusé : cette certification ne vous appartient pas");
        }
    }

//    private void notifyCertificationEvent(User user, String titre, String action) {
//        try {
//            realtimeService.notifyCertification(
//                    user.getPrenom(),
//                    user.getNom(),
//                    titre,
//                    action
//            );
//            realtimeService.pushStats();
//        } catch (Exception e) {
//            log.warn("Notification échouée : {}", e.getMessage());
//        }
//    }


    private CertificationDTO toDTO(Certification c) {
        return CertificationDTO.builder()
                .idCertification(c.getIdCertification())
                .titre(c.getTitre())
                .description(c.getDescription())
                .dateCertif(c.getDateCertif())
                .pdfBase64(fileService.encodePdfToDataUri(c.getPdfCertif()))
                .build();
    }
}