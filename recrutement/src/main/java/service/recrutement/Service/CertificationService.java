package service.recrutement.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import service.recrutement.Client.UserNotificationClient;
import service.recrutement.Entity.Certification;
import service.recrutement.Entity.dto.CertificationDto;
import service.recrutement.Entity.dto.PageResponseDto;
import service.recrutement.Repository.CertificationRepository;
import service.recrutement.Service.UserCVFile.FileService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificationService {

    CertificationRepository repository;
    FileService fileService;
    UserNotificationClient userNotificationClient;
public PageResponseDto<CertificationDto> getByUserIdPaged(
        String keycloakId, int page, int size,
        String sortBy, String sortDir, String titre) {

    Sort sort = "desc".equalsIgnoreCase(sortDir)
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<Certification> result = (titre != null && !titre.isBlank())
            ? repository.findByKeycloakIdAndTitreContainingIgnoreCase(keycloakId, titre, pageable)
            : repository.findByKeycloakId(keycloakId, pageable);

    return PageResponseDto.<CertificationDto>builder()
            .content(result.map(this::toDto).getContent())
            .page(result.getNumber())
            .size(result.getSize())
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .last(result.isLast())
            .build();
}
    public List<CertificationDto> getByUserId(String keycloakId) {
        return repository.findByKeycloakId(keycloakId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CertificationDto> getMyCertifications(String keycloakId) {
        return repository.findByKeycloakId(keycloakId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public CertificationDto getById(String certifId, String keycloakId) {
        return toDto(findAndAssertOwner(certifId, keycloakId));
    }

    private void notifyUserService(String keycloakId, String titre, String action) {
        try {
            userNotificationClient.notifyCertification(
                    new UserNotificationClient.CertificationNotifyRequest(keycloakId, titre, action));
            log.info("Notification envoyée : keycloakId={}, titre={}, action={}", keycloakId, titre, action);
        } catch (Exception e) {
            log.warn("Notification certification vers user-service échouée : {}", e.getMessage());
        }
    }

    public CertificationDto add(String keycloakId, CertificationDto dto) {
        // PDF optionnel : le formulaire d'ajout côté Angular ne l'impose pas
        byte[] pdfBytes = dto.getPdfBase64() != null ? fileService.decodeBase64(dto.getPdfBase64()) : null;

        Certification certif = Certification.builder()
                .keycloakId(keycloakId)
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .dateCertif(dto.getDateCertif() != null ? dto.getDateCertif() : LocalDate.now())
                .pdfCertif(pdfBytes)
                .createdAt(LocalDateTime.now())
                .build();

        Certification saved = repository.save(certif);
        log.info("Certification ajoutée : {} pour keycloakId={}", saved.getTitre(), keycloakId);
        notifyUserService(keycloakId, saved.getTitre(), "AJOUT");
        return toDto(saved);
    }

    public CertificationDto update(String certifId, String keycloakId, CertificationDto dto) {
        Certification certif = findAndAssertOwner(certifId, keycloakId);

        if (dto.getTitre() != null) certif.setTitre(dto.getTitre());
        if (dto.getDescription() != null) certif.setDescription(dto.getDescription());
        if (dto.getDateCertif() != null) certif.setDateCertif(dto.getDateCertif());

        if (dto.getPdfBase64() != null) {
            byte[] pdfBytes = fileService.decodeBase64(dto.getPdfBase64());
            if (pdfBytes != null) certif.setPdfCertif(pdfBytes);
        }

        Certification updated = repository.save(certif);
        log.info("Certification mise à jour : {}", certifId);
        notifyUserService(keycloakId, updated.getTitre(), "MODIFICATION");
        return toDto(updated);
    }

    public void delete(String certifId, String keycloakId) {
        Certification certif = findAndAssertOwner(certifId, keycloakId);
        String titre = certif.getTitre();
        repository.delete(certif);
        log.info("Certification supprimée : {}", certifId);
        notifyUserService(keycloakId, titre, "SUPPRESSION");
    }

    private Certification findAndAssertOwner(String certifId, String keycloakId) {
        Certification certif = repository.findById(certifId)
                .orElseThrow(() -> new RuntimeException("Certification introuvable : " + certifId));
        if (!certif.getKeycloakId().equals(keycloakId)) {
            throw new RuntimeException("Accès refusé : cette certification ne vous appartient pas");
        }
        return certif;
    }

    private CertificationDto toDto(Certification c) {
        return CertificationDto.builder()
                .idCertification(c.getIdCertification())
                .titre(c.getTitre())
                .description(c.getDescription())
                .dateCertif(c.getDateCertif())
                .pdfBase64(fileService.encodePdfToDataUri(c.getPdfCertif()))
                .build();
    }
}