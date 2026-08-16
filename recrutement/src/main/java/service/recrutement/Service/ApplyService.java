package service.recrutement.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import service.recrutement.Client.RankingClient;
import service.recrutement.Client.UserServiceClient;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.FileUser;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.*;
import service.recrutement.Exception.*;
import service.recrutement.Mail.RecrutementMail;
import service.recrutement.Repository.ApplicationRepository;
import service.recrutement.Service.UserCVFile.FileUserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplyService {

    private static final long MAX_CV_SIZE = 10L * 1024 * 1024;
    private static final long MAX_LETTRE_SIZE = 10L * 1024 * 1024;
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

    private final ApplicationRepository applicationRepository;
    private final FileUserService fileUserService;
    private final UserServiceClient userServiceClient;
    private final RecrutementMail recrutementMail;
    private final RankingClient rankingClient;
    private final PosteRecrutementService posteRecrutementService; // déjà injecté ailleurs

    public List<ApplicationDto> getCandidaturesClasseesPourPoste(String posteId) {
        PosteRecrutement poste = posteRecrutementService.getById(posteId);
        List<Application> candidatures = applicationRepository
                .findByPosteRecrutementIdOrderByDateCandidatureDesc(posteId);

        if (candidatures.isEmpty()) return List.of();

        MlPosteDto posteDto = MlPosteDto.builder()
                .competencesRequises(poste.getCompetencesRequises())
                .languesRequises(poste.getLanguesRequises())
                .anneesExperienceMin(poste.getAnneesExperienceMin())
                .niveauEtudeRequis(poste.getNiveauEtudeRequis())
                .typeContrat(poste.getTypeContrat() != null ? poste.getTypeContrat().name() : null)
                .workType(poste.getWorkType() != null ? poste.getWorkType().name() : null)
                .lieu(poste.getLieu())
                .salaire(poste.getSalaire() != null ? poste.getSalaire().doubleValue() : null)
                .build();

        List<MlCandidatDto> candidatsMl = candidatures.stream()
                .map(a -> fetchCandidatMlDto(a, poste))
                .toList();

        Map<String, Object> body = Map.of(
                "candidats", candidatsMl,
                "poste", posteDto
        );

        List<Map<String, Object>> scores;
        try {
            scores = rankingClient.scoreBatch(body);
            log.info("Scores reçus du ranking-service pour le poste {} : {}", posteId, scores);
        } catch (Exception e) {
            log.warn("Ranking-service injoignable pour le poste {} : {}", posteId, e.getMessage());
            scores = List.of();
        }

        Map<String, Double> scoreParCandidat = scores.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("candidatKeycloakId"),
                        m -> ((Number) m.get("score")).doubleValue()
                ));

        return candidatures.stream()
                .map(a -> {
                    ApplicationDto dto = toDtoPublic(a);
                    Double s = scoreParCandidat.get(a.getCandidatKeycloakId());
                    dto.setScoreMatching(s);
                    return dto;
                })
                .sorted(Comparator.comparing(
                        ApplicationDto::getScoreMatching,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private MlCandidatDto fetchCandidatMlDto(Application a, PosteRecrutement poste) {
        CandidatDto c = fetchCandidatInfo(a.getCandidatKeycloakId());
        log.info("DEBUG candidat={} → userServiceOk={} snapshotCompetences={}",
                a.getCandidatKeycloakId(), c != null, a.getCompetences());
        if (c == null) {
            // fallback sur le snapshot stocké dans la candidature elle-même
            return MlCandidatDto.builder()
                    .keycloakId(a.getCandidatKeycloakId())
                    .competences(a.getCompetences())
                    .langues(a.getLangues())
                    .anneesExperience(a.getAnneesExperienceCandidat())
                    .build();
        }
        return MlCandidatDto.builder()
                .keycloakId(a.getCandidatKeycloakId())
                .competences(c.getCompetences())
                .langues(c.getLangues())
                .anneesExperience(c.getAnneesExperience())
                .niveauEtude(c.getNiveauEtude())
                .typeContratSouhaite(c.getTypeContratSouhaite())
                .lieu(c.getLieu())
                .salaireAttendu(c.getSalaireAttendu())
                .certifications(c.getCertifications())
                .build();
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

        FileUser cvExistant = fileUserService.findCv(candidatKeycloakId)
                .orElseThrow(() -> new CvRequisException(
                        "Aucun CV enregistré sur ton profil. Téléverse un CV pour postuler."));

        CandidatDto candidat = fetchCandidatInfo(candidatKeycloakId);

        Application application = prepareApplicationForSubmission(
                candidatKeycloakId, poste, candidat, dto.getLettreMotivationTexte());
        application.setCvSnapshot(cvExistant.getCvUser());
        application.setCvSnapshotFileName(cvExistant.getCvFileName());
        application.setCvSnapshotContentType("application/pdf");

        attachLettreMotivationPdfSiPresente(application, lettreMotivationPdf);

        return saveCandidature(application, poste);
    }

    /**
     * Postuler avec un CV téléversé spécifiquement pour cette candidature.
     */
    @Transactional
    public ApplicationDto postulerAvecNouveauCv(
            String candidatKeycloakId, String posteId, MultipartFile cvFile,
            String lettreMotivationTexte, MultipartFile lettreMotivationPdf) {

        PosteRecrutement poste = getPosteOuverOuException(posteId);
        validateCvFile(cvFile);

        CandidatDto candidat = fetchCandidatInfo(candidatKeycloakId);

        Application application = prepareApplicationForSubmission(
                candidatKeycloakId, poste, candidat, lettreMotivationTexte);
        try {
            application.setCvSnapshot(cvFile.getBytes());
        } catch (Exception e) {
            throw new CvRequisException("Impossible de lire le fichier CV envoyé");
        }
        application.setCvSnapshotFileName(cvFile.getOriginalFilename());
        application.setCvSnapshotContentType(cvFile.getContentType());

        attachLettreMotivationPdfSiPresente(application, lettreMotivationPdf);

        return saveCandidature(application, poste);
    }

    /**
     * Retourne le document à sauvegarder pour une nouvelle candidature :
     * - s'il n'existe aucune candidature du candidat pour ce poste, en crée une nouvelle ;
     * - si une candidature RETIRE existe déjà (contrainte d'unicité candidat+poste),
     * elle est réactivée (repassée à EN_ATTENTE) au lieu de créer un doublon ;
     * - si une candidature active (tout autre statut) existe déjà, on refuse.
     */
    private Application prepareApplicationForSubmission(
            String candidatKeycloakId, PosteRecrutement poste, CandidatDto candidat, String lettreMotivationTexte) {

        Optional<Application> existante = applicationRepository
                .findByCandidatKeycloakIdAndPosteRecrutementId(candidatKeycloakId, poste.getIdPosteRecrutement());

        if (existante.isPresent()) {
            Application application = existante.get();

            if (application.getStatut() != ApplicationStatus.RETIRE) {
                throw new CandidatureExistanteException();
            }

            LocalDateTime now = LocalDateTime.now();
            application.setStatut(ApplicationStatus.EN_ATTENTE);
            application.setLettreMotivationTexte(lettreMotivationTexte);
            application.setLettreMotivationPdf(null);
            application.setLettreMotivationPdfFileName(null);
            application.setCommentaireRH(null);
            application.setDateCandidature(LocalDate.now());
            application.setDateDernierChangementStatut(now);

            if (candidat != null) {
                application.setNomComplet(candidat.getPrenom() + " " + candidat.getNom());
                application.setEmail(candidat.getEmail());
                application.setCompetences(candidat.getCompetences());
                application.setLangues(candidat.getLangues());
                application.setAnneesExperienceCandidat(candidat.getAnneesExperience());
            }

            application.getHistoriqueStatuts().add(StatusChange.builder()
                    .statut(ApplicationStatus.EN_ATTENTE)
                    .date(now)
                    .commentaire("Candidature redéposée par le candidat")
                    .auteurKeycloakId(candidatKeycloakId)
                    .build());

            return application;
        }

        return buildBaseApplication(candidatKeycloakId, poste, candidat, lettreMotivationTexte);
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
            throw new CvRequisException("La lettre de motivation ne doit pas dépasser 10 Mo");
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
            application.setCompetences(candidat.getCompetences());
            application.setLangues(candidat.getLangues());
            application.setAnneesExperienceCandidat(candidat.getAnneesExperience());
        }

        application.getHistoriqueStatuts().add(StatusChange.builder()
                .statut(ApplicationStatus.EN_ATTENTE)
                .date(now)
                .commentaire("Candidature déposée")
                .auteurKeycloakId(candidatKeycloakId)
                .build());

        return application;
    }

    private ApplicationDto saveCandidature(Application application, PosteRecrutement poste) {
        try {
            Application saved = applicationRepository.save(application);
            log.info("Candidature enregistrée : candidat={}, poste={}", saved.getCandidatKeycloakId(), poste.getTitre());
            notifyRecruteurNouvelleCandidature(poste, saved);
            return toDto(saved);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new CandidatureExistanteException();
        }
    }

    /**
     * Modification d'une candidature par son propriétaire — uniquement autorisée
     * tant qu'elle est EN_ATTENTE (avant toute action du RH).
     */
    @Transactional
    public ApplicationDto modifierCandidature(
            String idApplication, String candidatKeycloakId, String lettreMotivationTexte,
            MultipartFile cvFile, MultipartFile lettreMotivationPdf, boolean supprimerLettrePdf) {

        Application application = getApplicationOuException(idApplication);

        if (!application.getCandidatKeycloakId().equals(candidatKeycloakId)) {
            throw new AccesNonAutoriseException("Cette candidature ne vous appartient pas");
        }
        if (application.getStatut() != ApplicationStatus.EN_ATTENTE) {
            throw new TransitionStatutInvalideException(
                    "Vous ne pouvez modifier votre candidature que tant qu'elle est en attente");
        }

        application.setLettreMotivationTexte(lettreMotivationTexte);

        if (cvFile != null && !cvFile.isEmpty()) {
            validateCvFile(cvFile);
            try {
                application.setCvSnapshot(cvFile.getBytes());
            } catch (Exception e) {
                throw new CvRequisException("Impossible de lire le fichier CV envoyé");
            }
            application.setCvSnapshotFileName(cvFile.getOriginalFilename());
            application.setCvSnapshotContentType(cvFile.getContentType());
        }

        if (supprimerLettrePdf) {
            application.setLettreMotivationPdf(null);
            application.setLettreMotivationPdfFileName(null);
        } else if (lettreMotivationPdf != null && !lettreMotivationPdf.isEmpty()) {
            attachLettreMotivationPdfSiPresente(application, lettreMotivationPdf);
        }

        application.setDateDernierChangementStatut(LocalDateTime.now());
        Application saved = applicationRepository.save(application);
        log.info("Candidature {} modifiée par le candidat {}", idApplication, candidatKeycloakId);
        return toDto(saved);
    }

    /**
     * Retourne (bytes, fileName) — sécurisé : réservé au propriétaire de la candidature ou au RH.
     */
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

    @Transactional
    public ApplicationDto changerStatutParRH(
            String idApplication,
            ApplicationStatus nouveauStatut,
            String commentaireRH,
            String recruteurKeycloakId) {

        if (nouveauStatut == null) {
            throw new TransitionStatutInvalideException(
                    "Le nouveau statut est obligatoire"
            );
        }

        if (nouveauStatut == ApplicationStatus.REJETE
                && (commentaireRH == null || commentaireRH.isBlank())) {

            throw new TransitionStatutInvalideException(
                    "Un commentaire RH est obligatoire pour rejeter une candidature"
            );
        }

        Application application =
                getApplicationOuException(idApplication);

        changerStatutInterne(
                application,
                nouveauStatut,
                commentaireRH,
                recruteurKeycloakId
        );

        notifyCandidatChangementStatut(application);

        return toDto(application);
    }

  @Transactional
public ApplicationDto changerStatutSysteme(
        String idApplication,
        ApplicationStatus nouveauStatut,
        String commentaire,
        String auteurKeycloakId) {

    Application application =
            getApplicationOuException(idApplication);

    changerStatutInterne(
            application,
            nouveauStatut,
            commentaire,
            auteurKeycloakId
    );

    notifyCandidatChangementStatut(application);

    return toDto(application);
}
    private void changerStatutInterne(
            Application application,
            ApplicationStatus nouveauStatut,
            String commentaire,
            String auteurKeycloakId) {

        ApplicationStatus ancienStatut =
                application.getStatut();

        Set<ApplicationStatus> transitionsPossibles =
                TRANSITIONS_AUTORISEES.getOrDefault(
                        ancienStatut,
                        EnumSet.noneOf(ApplicationStatus.class)
                );

        if (!transitionsPossibles.contains(nouveauStatut)) {
            throw new TransitionStatutInvalideException(
                    "Transition impossible : "
                            + ancienStatut
                            + " → "
                            + nouveauStatut
            );
        }

        if (nouveauStatut == ApplicationStatus.REJETE
                && (commentaire == null || commentaire.isBlank())) {

            throw new TransitionStatutInvalideException(
                    "Un commentaire est obligatoire pour un rejet"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        application.setStatut(nouveauStatut);
        application.setDateDernierChangementStatut(now);

        if (commentaire != null && !commentaire.isBlank()) {
            application.setCommentaireRH(commentaire.trim());
        } else {
            application.setCommentaireRH(null);
        }

        if (application.getHistoriqueStatuts() == null) {
            application.setHistoriqueStatuts(new ArrayList<>());
        }

        application.getHistoriqueStatuts().add(
                StatusChange.builder()
                        .statut(nouveauStatut)
                        .date(now)
                        .commentaire(commentaire)
                        .auteurKeycloakId(auteurKeycloakId)
                        .build()
        );

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
            throw new CvRequisException("Le CV ne doit pas dépasser 10 Mo");
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