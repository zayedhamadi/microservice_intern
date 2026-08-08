package service.recrutement.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import service.recrutement.Client.UserServiceClient;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.StatusChange;
import service.recrutement.Entity.FileUser;
import service.recrutement.Entity.dto.ApplicationDto;
import service.recrutement.Entity.dto.ApplyRequestDto;
import service.recrutement.Entity.dto.CandidatDto;
import service.recrutement.Exception.*;
import service.recrutement.Mail.RecrutementMail;
import service.recrutement.Repository.ApplicationRepository;
import service.recrutement.Service.UserCVFile.FileUserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplyService {

    private final ApplicationRepository applicationRepository;
    private final PosteRecrutementService posteRecrutementService;
    private final FileUserService fileUserService;
    private final UserServiceClient userServiceClient;
    private final RecrutementMail recrutementMail;

    private static final long MAX_CV_SIZE = 5L * 1024 * 1024;
    private static final long MAX_LETTRE_SIZE = 5L * 1024 * 1024;


    private static final Map<ApplicationStatus, Set<ApplicationStatus>> TRANSITIONS_AUTORISEES = new EnumMap<>(ApplicationStatus.class);

    static {
        TRANSITIONS_AUTORISEES.put(ApplicationStatus.EN_ATTENTE,
                EnumSet.of(ApplicationStatus.SELECTIONNE, ApplicationStatus.REJETE, ApplicationStatus.RETIRE));
        TRANSITIONS_AUTORISEES.put(ApplicationStatus.SELECTIONNE,
                EnumSet.of(ApplicationStatus.EN_ENTRETIEN_RH, ApplicationStatus.REJETE, ApplicationStatus.RETIRE));
        TRANSITIONS_AUTORISEES.put(ApplicationStatus.EN_ENTRETIEN_RH,
                EnumSet.of(ApplicationStatus.EN_ENTRETIEN_TECHNIQUE, ApplicationStatus.REJETE, ApplicationStatus.RETIRE));
        TRANSITIONS_AUTORISEES.put(ApplicationStatus.EN_ENTRETIEN_TECHNIQUE,
                EnumSet.of(ApplicationStatus.EN_ENTRETIEN_FINAL, ApplicationStatus.REJETE, ApplicationStatus.RETIRE));
        TRANSITIONS_AUTORISEES.put(ApplicationStatus.EN_ENTRETIEN_FINAL,
                EnumSet.of(ApplicationStatus.ACCEPTE, ApplicationStatus.REJETE, ApplicationStatus.RETIRE));
        TRANSITIONS_AUTORISEES.put(ApplicationStatus.ACCEPTE, EnumSet.noneOf(ApplicationStatus.class));
        TRANSITIONS_AUTORISEES.put(ApplicationStatus.REJETE, EnumSet.noneOf(ApplicationStatus.class));
        TRANSITIONS_AUTORISEES.put(ApplicationStatus.RETIRE, EnumSet.noneOf(ApplicationStatus.class));
    }

    private void notifyRecruteurNouvelleCandidature(PosteRecrutement poste, Application application) {
        try {
            recrutementMail.sendCandidatureConfirmation(poste, application);
            recrutementMail.sendNewApplicationNotificationToRH(null, poste, application);
        } catch (Exception e) {
            log.warn("Notification échouée pour la candidature {} : {}",
                    application.getIdApplication(), e.getMessage());
        }
    }

    @Transactional
    public ApplicationDto postulerAvecCvExistant(
            String candidatKeycloakId, ApplyRequestDto dto, MultipartFile lettreMotivationPdf) {

        PosteRecrutement poste = getPosteOuverOuException(dto.getIdPosteRecrutement());
        assertPasDeCandidatureExistante(candidatKeycloakId, poste.getIdPosteRecrutement());

        FileUser cvExistant = fileUserService.findCv(candidatKeycloakId)
                .orElseThrow(() -> new CvRequisException(
                        "Aucun CV enregistré sur ton profil. Téléverse un CV pour postuler."));

        CandidatDto candidat = fetchCandidatInfo(candidatKeycloakId);

        Application application = buildBaseApplication(candidatKeycloakId, poste, candidat, dto.getLettreMotivationTexte());
        application.setCvSnapshot(cvExistant.getCvUser());
        application.setCvSnapshotFileName(cvExistant.getCvFileName());
        application.setCvSnapshotContentType("application/pdf");

        attachLettreMotivationPdfSiPresente(application, lettreMotivationPdf);

        return saveNouvelleCandidature(application, poste);
    }

    /** Postuler avec un CV téléversé spécifiquement pour cette candidature. */
    @Transactional
    public ApplicationDto postulerAvecNouveauCv(
            String candidatKeycloakId, String posteId, MultipartFile cvFile,
            String lettreMotivationTexte, MultipartFile lettreMotivationPdf) {

        PosteRecrutement poste = getPosteOuverOuException(posteId);
        assertPasDeCandidatureExistante(candidatKeycloakId, poste.getIdPosteRecrutement());
        validateCvFile(cvFile);

        CandidatDto candidat = fetchCandidatInfo(candidatKeycloakId);

        Application application = buildBaseApplication(candidatKeycloakId, poste, candidat, lettreMotivationTexte);
        try {
            application.setCvSnapshot(cvFile.getBytes());
        } catch (Exception e) {
            throw new CvRequisException("Impossible de lire le fichier CV envoyé");
        }
        application.setCvSnapshotFileName(cvFile.getOriginalFilename());
        application.setCvSnapshotContentType(cvFile.getContentType());

        attachLettreMotivationPdfSiPresente(application, lettreMotivationPdf);

        return saveNouvelleCandidature(application, poste);
    }

    private void attachLettreMotivationPdfSiPresente(Application application, MultipartFile lettreMotivationPdf) {
        if (lettreMotivationPdf == null || lettreMotivationPdf.isEmpty()) {
            return;
        }
        validateLettreMotivationPdf(lettreMotivationPdf);
        try {
            application.setLettreMotivationPdf(lettreMotivationPdf.getBytes());
            application.setLettreMotivationPdfFileName(lettreMotivationPdf.getOriginalFilename());
        } catch (Exception e) {
            throw new CvRequisException("Impossible de lire le fichier de lettre de motivation");
        }
    }

    private void validateLettreMotivationPdf(MultipartFile file) {
        String contentType = file.getContentType();
        if (!"application/pdf".equals(contentType)) {
            throw new CvRequisException("La lettre de motivation doit être un fichier PDF");
        }
        if (file.getSize() > MAX_LETTRE_SIZE) {
            throw new CvRequisException("La lettre de motivation ne doit pas dépasser 5 Mo");
        }
    }

    private Application buildBaseApplication(
            String candidatKeycloakId, PosteRecrutement poste, CandidatDto candidat, String lettreMotivationTexte) {

        LocalDateTime now = LocalDateTime.now();

        Application application = Application.builder()
                .candidatKeycloakId(candidatKeycloakId)
                .posteRecrutementId(poste.getIdPosteRecrutement())
                .lettreMotivationTexte(lettreMotivationTexte)
                .statut(ApplicationStatus.EN_ATTENTE)
                .dateCandidature(LocalDate.now())
                .dateDernierChangementStatut(now)
                .build();

        if (candidat != null) {
            application.setNomComplet(candidat.getPrenom() + " " + candidat.getNom());
            application.setEmail(candidat.getEmail());
        }

        application.getHistoriqueStatuts().add(StatusChange.builder()
                .statut(ApplicationStatus.EN_ATTENTE)
                .date(now)
                .commentaire("Candidature déposée")
                .auteurKeycloakId(candidatKeycloakId)
                .build());

        return application;
    }

    private ApplicationDto saveNouvelleCandidature(Application application, PosteRecrutement poste) {
        try {
            Application saved = applicationRepository.save(application);
            log.info("Nouvelle candidature : candidat={}, poste={}", saved.getCandidatKeycloakId(), poste.getTitre());
            notifyRecruteurNouvelleCandidature(poste, saved);
            return toDto(saved);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new CandidatureExistanteException();
        }
    }

    /** Retourne (bytes, fileName) — sécurisé : réservé au propriétaire de la candidature ou au RH. */
    public Application getApplicationPourTelechargementLettre(String idApplication, String requesterKeycloakId, boolean isRH) {
        Application application = getApplicationOuException(idApplication);

        if (!isRH && !application.getCandidatKeycloakId().equals(requesterKeycloakId)) {
            throw new AccesNonAutoriseException("Accès non autorisé à cette candidature");
        }
        if (application.getLettreMotivationPdf() == null) {
            throw new CandidatureNotFoundException("Aucune lettre de motivation PDF pour cette candidature");
        }
        return application;
    }

    public Optional<ApplicationDto> getMaCandidaturePourPoste(String candidatKeycloakId, String posteId) {
        return applicationRepository
                .findByCandidatKeycloakIdAndPosteRecrutementId(candidatKeycloakId, posteId)
                .map(this::toDto);
    }

    public List<ApplicationDto> getMesCandidatures(String candidatKeycloakId) {
        return applicationRepository.findByCandidatKeycloakIdOrderByDateCandidatureDesc(candidatKeycloakId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void retirerCandidature(String idApplication, String candidatKeycloakId) {
        Application application = getApplicationOuException(idApplication);

        if (!application.getCandidatKeycloakId().equals(candidatKeycloakId)) {
            throw new AccesNonAutoriseException("Cette candidature ne vous appartient pas");
        }

        changerStatutInterne(application, ApplicationStatus.RETIRE,
                "Retirée par le candidat", candidatKeycloakId);

        log.info("Candidature {} retirée par le candidat {}", idApplication, candidatKeycloakId);
    }

    public List<ApplicationDto> getCandidaturesPourPoste(String posteId) {
        return applicationRepository.findByPosteRecrutementIdOrderByDateCandidatureDesc(posteId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public long countCandidaturesPourPoste(String posteId) {
        return applicationRepository.countByPosteRecrutementId(posteId);
    }

    /**
     * Décision manuelle du RH : accepter le dossier (EN_ATTENTE -> SELECTIONNE)
     * ou le refuser (-> REJETE) à n'importe quelle étape. La progression à travers
     * les 3 entretiens n'utilise PAS ce point d'entrée : elle est pilotée par
     * InterviewService via {@link #changerStatutSysteme}, ce qui garantit qu'on ne
     * peut pas « sauter » une étape d'entretien depuis l'API RH générique.
     */
    @Transactional
    public ApplicationDto changerStatutParRH(
            String idApplication, ApplicationStatus nouveauStatut, String commentaireRH, String recruteurKeycloakId) {

        Application application = getApplicationOuException(idApplication);
        changerStatutInterne(application, nouveauStatut, commentaireRH, recruteurKeycloakId);

        notifyCandidatChangementStatut(application);

        return toDto(application);
    }

    /**
     * Point d'entrée utilisé par InterviewService pour faire progresser une
     * candidature suite à la planification ou au résultat d'un entretien.
     * Passe par la même validation de transitions que changerStatutParRH.
     */
    @Transactional
    public ApplicationDto changerStatutSysteme(
            String idApplication, ApplicationStatus nouveauStatut, String commentaire, String auteurKeycloakId) {

        Application application = getApplicationOuException(idApplication);
        changerStatutInterne(application, nouveauStatut, commentaire, auteurKeycloakId);

        notifyCandidatChangementStatut(application);

        return toDto(application);
    }

    private void changerStatutInterne(
            Application application, ApplicationStatus nouveauStatut, String commentaire, String auteurKeycloakId) {

        ApplicationStatus ancienStatut = application.getStatut();
        Set<ApplicationStatus> transitionsPossibles = TRANSITIONS_AUTORISEES.getOrDefault(
                ancienStatut, EnumSet.noneOf(ApplicationStatus.class));

        if (!transitionsPossibles.contains(nouveauStatut)) {
            throw new TransitionStatutInvalideException(
                    "Transition impossible : " + ancienStatut + " → " + nouveauStatut);
        }

        LocalDateTime now = LocalDateTime.now();
        application.setStatut(nouveauStatut);
        application.setDateDernierChangementStatut(now);
        if (commentaire != null && !commentaire.isBlank()) {
            application.setCommentaireRH(commentaire);
        }

        application.getHistoriqueStatuts().add(StatusChange.builder()
                .statut(nouveauStatut)
                .date(now)
                .commentaire(commentaire)
                .auteurKeycloakId(auteurKeycloakId)
                .build());

        applicationRepository.save(application);
    }

    private void notifyCandidatChangementStatut(Application application) {
        try {
            if (application.getEmail() == null) return;
            PosteRecrutement poste = posteRecrutementService.getById(application.getPosteRecrutementId());
            recrutementMail.sendApplicationStatusChanged(poste, application);
        } catch (Exception e) {
            log.warn("Notification candidat échouée pour la candidature {} : {}",
                    application.getIdApplication(), e.getMessage());
        }
    }

    private PosteRecrutement getPosteOuverOuException(String posteId) {
        PosteRecrutement poste;
        try {
            poste = posteRecrutementService.getById(posteId);
        } catch (Exception e) {
            throw new PosteRecrutementNotFoundException(posteId);
        }

        if (!poste.isOuvert()) {
            throw new PosteFermeException(poste.getTitre());
        }
        return poste;
    }

    private void assertPasDeCandidatureExistante(String candidatKeycloakId, String posteId) {
        if (applicationRepository.existsByCandidatKeycloakIdAndPosteRecrutementId(candidatKeycloakId, posteId)) {
            throw new CandidatureExistanteException();
        }
    }

    private void validateCvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CvRequisException("Le fichier CV est requis");
        }
        String contentType = file.getContentType();
        boolean typeValide = contentType != null && (
                contentType.equals("application/pdf") ||
                        contentType.equals("application/msword") ||
                        contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
        if (!typeValide) {
            throw new CvRequisException("Formats acceptés : PDF, DOC, DOCX");
        }
        if (file.getSize() > MAX_CV_SIZE) {
            throw new CvRequisException("Le CV ne doit pas dépasser 5 Mo");
        }
    }

    private CandidatDto fetchCandidatInfo(String candidatKeycloakId) {
        try {
            return userServiceClient.getCandidatByKeycloakId(candidatKeycloakId);
        } catch (Exception e) {
            log.warn("Impossible de récupérer les infos du candidat {} depuis user-service : {}",
                    candidatKeycloakId, e.getMessage());
            return null;
        }
    }

    /** Public pour permettre à InterviewService de récupérer la candidature liée à un entretien. */
    public Application getApplicationOuException(String idApplication) {
        return applicationRepository.findById(idApplication)
                .orElseThrow(() -> new CandidatureNotFoundException(idApplication));
    }

    public ApplicationDto toDtoPublic(Application application) {
        return toDto(application);
    }

    private ApplicationDto toDto(Application a) {
        return ApplicationDto.builder()
                .idApplication(a.getIdApplication())
                .candidatKeycloakId(a.getCandidatKeycloakId())
                .posteRecrutementId(a.getPosteRecrutementId())
                .cvSnapshotFileName(a.getCvSnapshotFileName())
                .lettreMotivationTexte(a.getLettreMotivationTexte())
                .lettreMotivationPdfPresente(a.getLettreMotivationPdf() != null)
                .lettreMotivationPdfFileName(a.getLettreMotivationPdfFileName())
                .nomComplet(a.getNomComplet())
                .email(a.getEmail())
                .telephone(a.getTelephone())
                .specialite(a.getSpecialite())
                .formation(a.getFormation())
                .commentaireRH(a.getCommentaireRH())
                .experience(a.getExperience())
                .anneesExperienceCandidat(a.getAnneesExperienceCandidat())
                .competences(a.getCompetences())
                .langues(a.getLangues())
                .statut(a.getStatut())
                .dateCandidature(a.getDateCandidature())
                .dateDernierChangementStatut(a.getDateDernierChangementStatut())
                .scoreMatching(a.getScoreMatching())
                .historiqueStatuts(a.getHistoriqueStatuts())
                .build();
    }
}