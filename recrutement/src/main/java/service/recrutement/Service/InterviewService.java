package service.recrutement.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.recrutement.Client.UserServiceClient;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.*;
import service.recrutement.Entity.Interview;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.InterviewStatsDto;
import service.recrutement.Entity.dto.InterviewDto;
import service.recrutement.Entity.dto.PlanifierEntretienDto;
import service.recrutement.Entity.dto.ResultatEntretienDto;
import service.recrutement.Entity.Event.ApplicationInterviewsShouldCloseEvent;
import service.recrutement.Exception.AccesNonAutoriseException;
import service.recrutement.Exception.CandidatureNotFoundException;
import service.recrutement.Exception.TransitionStatutInvalideException;
import service.recrutement.Mail.RecrutementMail;
import service.recrutement.Repository.ApplicationRepository;
import service.recrutement.Repository.InterviewRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import service.recrutement.Entity.dto.CandidatEntretienTechniqueDto;
import service.recrutement.Entity.dto.PosteEntretiensTechniquesDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import service.recrutement.Entity.dto.CandidatDto;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewService {
    private final Map<String, String> displayNameCache = new ConcurrentHashMap<>();
    private final Set<String> directoryNameMisses = ConcurrentHashMap.newKeySet();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final ApplyService applyService;
    private final PosteRecrutementService posteRecrutementService;
    private final RecrutementMail recrutementMail;
    private final UserServiceClient userServiceClient;
    private final GoogleMeetService googleMeetService;


    private static final Set<InterviewStatus> STATUTS_ACTIFS =
            EnumSet.of(InterviewStatus.PLANIFIE, InterviewStatus.REPORTE);

    // ==================== ENTRETIENS LIBRES (créés depuis le calendrier) ====================

@Transactional
public InterviewDto createLibre(InterviewDto dto, String auteurKeycloakId) {
    validerDtoLibre(dto);

    LocalDateTime now = LocalDateTime.now();
    Interview interview = fromDto(dto);
    interview.setIdInterview(null);
    interview.setSource(InterviewSource.LIBRE);
    interview.setApplicationId(null);
    interview.setType(null);
    interview.setRecruteurKeycloakId(auteurKeycloakId);
    interview.setInterviewerName(resolveDisplayName(auteurKeycloakId, dto.getInterviewerName()));

    // ==================== GÉNÉRATION AUTOMATIQUE DU LIEN MEET ====================
    if (dto.getMode() == InterviewMode.DISTANCIEL) {
        String genere = googleMeetService.genererLienMeet(
                "Entretien - " + dto.getCandidateName(),
                dto.getCandidateEmail(),
                null,
                interview.getDateEntretien()
        );
        interview.setLienVisio(normaliser(genere != null ? genere : dto.getMeetingLink()));
    }
    // ========================================================================

    interview.setDateCreation(now);
    interview.setDateModification(now);

    Interview saved = interviewRepository.save(interview);

    // ==================== ENVOI DE LA CONVOCATION ====================
    try {
        recrutementMail.sendEntretienConvocationLibre(saved);
    } catch (Exception e) {
        log.error("Erreur lors de l'envoi de la convocation pour l'entretien libre {}", saved.getIdInterview(), e);
    }
    // ===================================================================

    return toDto(saved);
}
@Transactional
public InterviewDto updateLibre(String id, InterviewDto dto, String auteurKeycloakId) {
    Interview existant = getEntityOuException(id);
    validerDtoLibre(dto);

    Interview maj = fromDto(dto);
    maj.setIdInterview(existant.getIdInterview());
    maj.setVersion(existant.getVersion());
    maj.setSource(existant.getSource());
    maj.setApplicationId(existant.getApplicationId());
    maj.setType(existant.getType());
    maj.setCandidatKeycloakId(existant.getCandidatKeycloakId());
    maj.setRecruteurKeycloakId(
            existant.getRecruteurKeycloakId() != null ? existant.getRecruteurKeycloakId() : auteurKeycloakId);
    maj.setInterviewerName(
            existant.getInterviewerName() != null && existant.getRecruteurKeycloakId() != null
                    ? maj.getInterviewerName()
                    : resolveDisplayName(auteurKeycloakId, maj.getInterviewerName()));

    // ==================== GÉNÉRATION AUTOMATIQUE DU LIEN MEET ====================
    if (maj.getMode() == InterviewMode.DISTANCIEL
            && (existant.getLienVisio() == null || existant.getMode() != InterviewMode.DISTANCIEL)) {
        String genere = googleMeetService.genererLienMeet(
                "Entretien - " + maj.getCandidateName(),
                maj.getCandidateEmail(),
                null,
                maj.getDateEntretien()
        );
        maj.setLienVisio(normaliser(genere != null ? genere : dto.getMeetingLink()));
    } else if (maj.getMode() == InterviewMode.DISTANCIEL) {
        maj.setLienVisio(existant.getLienVisio()); // garde le lien existant, ne régénère pas
    }
    // ========================================================================

    maj.setDateCreation(existant.getDateCreation());
    maj.setDateModification(LocalDateTime.now());

    Interview saved = interviewRepository.save(maj);

    // ==================== ENVOI DE LA CONVOCATION (MISE À JOUR) ====================
    try {
        recrutementMail.sendEntretienConvocationLibre(saved);
    } catch (Exception e) {
        log.error("Erreur lors de l'envoi de la convocation (mise à jour) pour l'entretien libre {}", saved.getIdInterview(), e);
    }
    // =================================================================================

    return toDto(saved);
}

    /**
     * BUG FIX : l'ancienne implémentation supprimait n'importe quel entretien,
     * y compris ceux issus du workflow de candidature (source = CANDIDATURE),
     * ce qui laissait l'Application bloquée dans un statut EN_ENTRETIEN_*
     * sans qu'aucun entretien n'existe plus, et détruisait toute trace pour
     * l'audit. Seuls les entretiens LIBRE peuvent désormais être supprimés ;
     * un entretien de candidature doit être annulé via {@link #annulerEntretien}
     * pour conserver la traçabilité et faire revenir la candidature à un
     * statut cohérent.
     */
    @Transactional
    public void delete(String id) {
        Interview interview = getEntityOuException(id);
        if (interview.getSource() != InterviewSource.LIBRE) {
            throw new TransitionStatutInvalideException(
                    "Un entretien issu d'une candidature ne peut pas être supprimé : "
                            + "utilisez l'annulation pour conserver la traçabilité");
        }
        interviewRepository.deleteById(id);
    }

    private void validerDtoLibre(InterviewDto dto) {
        if (estVide(dto.getCandidateName()))
            throw new TransitionStatutInvalideException("Le nom du candidat est requis");
        if (estVide(dto.getPosteRecrutement())) throw new TransitionStatutInvalideException("Le poste est requis");
        if (estVide(dto.getInterviewerName())) throw new TransitionStatutInvalideException("L'interviewer est requis");
        if (estVide(dto.getInterviewDate()))
            throw new TransitionStatutInvalideException("La date de l'entretien est requise");
        if (estVide(dto.getStartTime())) throw new TransitionStatutInvalideException("L'heure de début est requise");
        if (estVide(dto.getEndTime())) throw new TransitionStatutInvalideException("L'heure de fin est requise");
        if (dto.getMode() == null) throw new TransitionStatutInvalideException("Le mode est requis");
        if (dto.getStatus() == null) throw new TransitionStatutInvalideException("Le statut est requis");
    }

    // ==================== LECTURE ====================

    public List<PosteEntretiensTechniquesDto> getEntretiensTechniquesParPoste() {
        // 1) Source of truth: candidatures actuellement en entretien technique
        List<Application> candidatures = applicationRepository
                .findByStatut(ApplicationStatus.EN_ENTRETIEN_TECHNIQUE);

        if (candidatures.isEmpty()) return List.of();

        // 2) Interviews techniques déjà planifiées, indexées par applicationId.
        //    BUG FIX : on ne garde que celles actives (PLANIFIE/REPORTE), sinon un
        //    entretien TERMINE/ANNULE d'un cycle précédent pouvait s'afficher à la
        //    place de l'entretien réellement en cours.
        Map<String, Interview> interviewParApplication = interviewRepository
                .findByTypeAndSource(InterviewType.TECHNIQUE, InterviewSource.CANDIDATURE)
                .stream()
                .filter(i -> STATUTS_ACTIFS.contains(i.getStatut()))
                .collect(Collectors.toMap(
                        Interview::getApplicationId,
                        i -> i,
                        (a, b) -> a.getDateCreation().isAfter(b.getDateCreation()) ? a : b // garde la + récente
                ));

        Map<String, PosteEntretiensTechniquesDto> parPoste = new LinkedHashMap<>();

        for (Application app : candidatures) {
            String posteId = app.getPosteRecrutementId() != null ? app.getPosteRecrutementId() : "inconnu";

            PosteEntretiensTechniquesDto dto = parPoste.computeIfAbsent(posteId, k -> {
                String titre = null, departement = null;
                try {
                    PosteRecrutement p = posteRecrutementService.getById(posteId);
                    titre = p.getTitre();
                    departement = p.getDepartementNom();
                } catch (Exception ignored) {
                    // poste supprimé ou introuvable : on affiche quand même le candidat
                }
                return PosteEntretiensTechniquesDto.builder()
                        .posteId(posteId)
                        .posteTitre(titre)
                        .departementNom(departement)
                        .candidats(new ArrayList<>())
                        .build();
            });

            Interview interview = interviewParApplication.get(app.getIdApplication());
            dto.getCandidats().add(toCandidatTechniqueDto(app, interview));
        }

        parPoste.values().forEach(p -> p.setNombreCandidats(p.getCandidats().size()));

        List<PosteEntretiensTechniquesDto> resultat = new ArrayList<>(parPoste.values());
        resultat.sort(Comparator.comparing(PosteEntretiensTechniquesDto::getPosteTitre,
                Comparator.nullsLast(String::compareTo)));
        return resultat;
    }

    private CandidatEntretienTechniqueDto toCandidatTechniqueDto(Application app, Interview e) {
        if (e == null) {
            // Pas encore planifié : on l'affiche quand même, avec un statut "à planifier"
            return CandidatEntretienTechniqueDto.builder()
                    .applicationId(app.getIdApplication())
                    .candidatKeycloakId(app.getCandidatKeycloakId())
                    .candidateName(app.getNomComplet())
                    .candidateEmail(app.getEmail())
                    .status(InterviewStatus.PLANIFIE) // ou un statut dédié "A_PLANIFIER" si vous en ajoutez un
                    .build();
        }
        return toCandidatTechniqueDto(e);
    }

    private CandidatEntretienTechniqueDto toCandidatTechniqueDto(Interview e) {
        LocalDate date = e.getDateEntretien() != null ? e.getDateEntretien().toLocalDate() : null;
        LocalTime start = e.getDateEntretien() != null ? e.getDateEntretien().toLocalTime() : null;
        LocalDateTime fin = e.getDateFinEntretien() != null ? e.getDateFinEntretien()
                : (e.getDateEntretien() != null ? e.getDateEntretien().plusHours(1) : null);

        return CandidatEntretienTechniqueDto.builder()
                .interviewId(e.getIdInterview())
                .applicationId(e.getApplicationId())
                .candidatKeycloakId(e.getCandidatKeycloakId())
                .candidateName(e.getCandidateName())
                .candidateEmail(e.getCandidateEmail())
                .interviewDate(date != null ? date.toString() : null)
                .startTime(start != null ? start.format(TIME_FMT) : null)
                .endTime(fin != null ? fin.toLocalTime().format(TIME_FMT) : null)
                .mode(e.getMode())
                .location(e.getLieu())
                .meetingLink(e.getLienVisio())
                .status(e.getStatut())
                .resultat(e.getResultat())
                .notes(e.getNotes())
                .interviewerName(e.getInterviewerName())
                .build();
    }

    public List<InterviewDto> getAllCandidature() {
        return interviewRepository.findBySource(InterviewSource.CANDIDATURE).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<InterviewDto> getAll() {
        return interviewRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public InterviewDto getById(String id) {
        return toDto(getEntityOuException(id));
    }

    public List<InterviewStatsDto> getStats() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (var s : InterviewStatus.values()) counts.put(s.name(), 0L);
        counts.put("INCONNU", 0L);
        interviewRepository.findAll().forEach(i -> {
            String k = i.getStatut() != null ? i.getStatut().name() : "INCONNU";
            counts.merge(k, 1L, Long::sum);
        });
        return counts.entrySet().stream()
                .map(e -> InterviewStatsDto.builder().status(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());
    }

    // ==================== WORKFLOW CANDIDATURE ====================

    @Transactional
    public InterviewDto planifierEntretien(
            String idApplication, InterviewType type, PlanifierEntretienDto dto, String auteurKeycloakId) {

        if (type == null) {
            throw new TransitionStatutInvalideException("Le type d'entretien est obligatoire");
        }

        Application application = applyService.getApplicationOuException(idApplication);

        ApplicationStatus statutRequis = statutRequisPourPlanifier(type);
        if (application.getStatut() != statutRequis) {
            throw new TransitionStatutInvalideException(
                    "Impossible de planifier l'entretien " + libelle(type)
                            + ". Statut actuel : " + application.getStatut()
                            + ". Statut attendu : " + statutRequis);
        }

        validerPlanification(type, dto);

        int cycle = application.getCycleCandidature() != null ? application.getCycleCandidature() : 1;
        String slotKey = calculerActiveSlotKey(idApplication, type, cycle);

        // Contrôle "fast fail" pour un message d'erreur clair côté UI. Il reste
        // intrinsèquement racy pris isolément (check-then-act) : la garantie
        // définitive contre les doublons est l'index unique sparse sur
        // Interview.activeSlotKey, voir le catch(DuplicateKeyException) plus bas.
        boolean dejaPlanifie = interviewRepository.existsByApplicationIdAndTypeAndStatutIn(
                idApplication, type, List.of(InterviewStatus.PLANIFIE, InterviewStatus.REPORTE));

        if (dejaPlanifie) {
            throw new TransitionStatutInvalideException(
                    "Un entretien " + libelle(type) + " est déjà planifié pour cette candidature");
        }

        PosteRecrutement poste = posteRecrutementService.getById(application.getPosteRecrutementId());

        // ==================== GÉNÉRATION AUTOMATIQUE DU LIEN GOOGLE MEET ====================
        // Applicable aussi bien quand c'est un RH (rh-initial, rh-final) qu'un EMPLOYEE
        // (technique) qui planifie, puisque les deux passent par cette même méthode.
        String lienVisioFinal = dto.getLienVisio();
        if (dto.getMode() == InterviewMode.DISTANCIEL) {
            String genere = googleMeetService.genererLienMeet(
                    "Entretien " + libelle(type) + " - " + application.getNomComplet(),
                    application.getEmail(),
                    null, // email du recruteur, à brancher via UserServiceClient si disponible
                    dto.getDateEntretien()
            );
            // Fallback : si Google Meet indisponible/non configuré, on garde le lien manuel s'il y en a un
            lienVisioFinal = (genere != null) ? genere : dto.getLienVisio();
        }
        // ========================================================================

        LocalDateTime maintenant = LocalDateTime.now();

        Interview interview = Interview.builder()
                .source(InterviewSource.CANDIDATURE)
                .applicationId(idApplication)
                .candidatKeycloakId(application.getCandidatKeycloakId())
                .candidateName(application.getNomComplet())
                .candidateEmail(application.getEmail())
                .posteRecrutement(poste.getTitre())
                .posteId(poste.getIdPosteRecrutement())
                .recruteurKeycloakId(auteurKeycloakId)
                // BUG FIX : on stockait l'UUID Keycloak brut comme "nom" affiché.
                .interviewerName(resolveDisplayName(auteurKeycloakId, null))
                .type(type)
                .mode(dto.getMode())
                .dateEntretien(dto.getDateEntretien())
                .dateFinEntretien(dto.getDateEntretien().plusHours(1))
                .lieu(normaliser(dto.getLieu()))
                .lienVisio(normaliser(lienVisioFinal)) // ← utilise le lien généré (ou fallback manuel)
                .statut(InterviewStatus.PLANIFIE)
                .dateCreation(maintenant)
                .dateModification(maintenant)
                .cycleCandidature(cycle)
                .activeSlotKey(slotKey)
                .build();

        Interview saved;
        try {
            saved = interviewRepository.save(interview);
        } catch (DuplicateKeyException e) {
            // BUG FIX (race condition) : garantie ultime, atomique côté base,
            // qu'on ne peut pas créer deux entretiens actifs identiques même
            // en cas d'appels concurrents ayant tous les deux passé le
            // contrôle "exists" ci-dessus.
            throw new TransitionStatutInvalideException(
                    "Un entretien " + libelle(type) + " est déjà en cours de planification pour cette candidature");
        }

        ApplicationStatus nouveauStatut = statutEnCoursPour(type);
        if (application.getStatut() != nouveauStatut) {
            applyService.changerStatutSysteme(idApplication, nouveauStatut,
                    "Entretien " + libelle(type) + " planifié", auteurKeycloakId);
        }

        envoyerConvocation(application, saved);

        return toDto(saved);
    }

    private void validerPlanification(InterviewType type, PlanifierEntretienDto dto) {
        if (dto == null)
            throw new TransitionStatutInvalideException("Les informations de l'entretien sont obligatoires");
        if (dto.getMode() == null)
            throw new TransitionStatutInvalideException("Le mode de l'entretien est obligatoire");
        if (dto.getDateEntretien() == null)
            throw new TransitionStatutInvalideException("La date de l'entretien est obligatoire");
        if (!dto.getDateEntretien().isAfter(LocalDateTime.now())) {
            throw new TransitionStatutInvalideException("La date de l'entretien doit être dans le futur");
        }
        if (dto.getMode() == InterviewMode.TELEPHONIQUE && type != InterviewType.RH_INITIAL) {
            throw new TransitionStatutInvalideException("Le téléphone est autorisé uniquement pour l'entretien RH initial");
        }
        if (dto.getMode() == InterviewMode.PRESENTIEL && estVide(dto.getLieu())) {
            throw new TransitionStatutInvalideException("Le lieu est obligatoire pour un entretien présentiel");
        }
        // BUG FIX : le lien de visioconférence n'est plus exigé du client en DISTANCIEL,
        // il est désormais généré automatiquement côté serveur via GoogleMeetService
        // (avec fallback sur dto.getLienVisio() si la génération échoue). On ne bloque
        // donc plus la planification si le champ est vide.
    }

    /**
     * BUG FIX : contrôle d'autorisation absent auparavant — n'importe quel
     * utilisateur authentifié pouvait clôturer le résultat de l'entretien de
     * quelqu'un d'autre. Seul l'intervenant assigné (recruteurKeycloakId)
     * peut enregistrer le résultat, sauf si isRH=true (rôle RH avec droit
     * de passer outre, à adapter selon votre modèle de rôles).
     * <p>
     * NB : le contrôleur appelant cette méthode doit désormais transmettre
     * isRH (ex: via les rôles Keycloak du principal courant).
     */
    @Transactional
    public InterviewDto enregistrerResultat(
            String idInterview,
            ResultatEntretienDto dto,
            String auteurKeycloakId,
            boolean isRH) {

        if (dto == null || dto.getResultat() == null) {
            throw new TransitionStatutInvalideException(
                    "Le résultat de l'entretien est obligatoire");
        }

        Interview interview = getEntityOuException(idInterview);

        // =========================================================
        // CONTRÔLE D'AUTORISATION
        // =========================================================

        if (interview.getType() == InterviewType.RH_INITIAL
                || interview.getType() == InterviewType.RH_FINAL) {

            // RH_INITIAL et RH_FINAL → RH uniquement
            if (!isRH) {
                throw new AccesNonAutoriseException(
                        "Seul un RH peut enregistrer le résultat de cet entretien");
            }

        } else if (interview.getType() == InterviewType.TECHNIQUE) {

            // TECHNIQUE → Employee assigné uniquement
            if (interview.getRecruteurKeycloakId() == null
                    || !interview.getRecruteurKeycloakId().equals(auteurKeycloakId)) {

                throw new AccesNonAutoriseException(
                        "Seul l'employé assigné à cet entretien technique peut enregistrer le résultat");
            }
        }

        // =========================================================
        // VÉRIFICATION DU STATUT
        // =========================================================

        if (interview.getStatut() != InterviewStatus.PLANIFIE
                && interview.getStatut() != InterviewStatus.REPORTE) {

            throw new TransitionStatutInvalideException(
                    "Cet entretien ne peut plus recevoir de résultat");
        }

        // =========================================================
        // VÉRIFICATION DU RÉSULTAT
        // =========================================================

        if (dto.getResultat() == InterviewResult.ECHOUE
                && estVide(dto.getNotes())) {

            throw new TransitionStatutInvalideException(
                    "Une note est obligatoire lorsqu'un entretien est échoué");
        }

        // =========================================================
        // TERMINER L'ENTRETIEN
        // =========================================================

        interview.setStatut(InterviewStatus.TERMINE);
        interview.setResultat(dto.getResultat());
        interview.setNotes(normaliser(dto.getNotes()));
        interview.setDateModification(LocalDateTime.now());

        // Libération du créneau anti-doublon
        interview.setActiveSlotKey(null);

        Interview saved = interviewRepository.save(interview);

        // =========================================================
        // MISE À JOUR DE LA CANDIDATURE
        // =========================================================

        if (saved.getApplicationId() != null) {

            if (dto.getResultat() == InterviewResult.ECHOUE) {

                String commentaire =
                        "Entretien " + libelle(saved.getType()) + " non concluant";

                if (!estVide(dto.getNotes())) {
                    commentaire += " : " + dto.getNotes().trim();
                }

                applyService.changerStatutSysteme(
                        saved.getApplicationId(),
                        ApplicationStatus.REJETE,
                        commentaire,
                        auteurKeycloakId
                );

            } else {

                ApplicationStatus prochainStatut =
                        statutApresReussite(saved.getType());

                applyService.changerStatutSysteme(
                        saved.getApplicationId(),
                        prochainStatut,
                        "Entretien " + libelle(saved.getType()) + " réussi",
                        auteurKeycloakId
                );
            }
        }

        return toDto(saved);
    }

    /**
     * BUG FIX : transition ANNULE absente du service alors que le enum
     * InterviewStatus la prévoit. Annule un entretien de candidature encore
     * actif, libère le créneau anti-doublon et fait revenir la candidature
     * au statut "éligible à planifier" du type concerné, pour qu'un nouvel
     * entretien puisse être replanifié.
     */
    @Transactional
    public InterviewDto annulerEntretien(String idInterview, String motif, String auteurKeycloakId, boolean isRH) {
        Interview interview = getEntityOuException(idInterview);

        if (!isRH
                && interview.getRecruteurKeycloakId() != null
                && !interview.getRecruteurKeycloakId().equals(auteurKeycloakId)) {
            throw new AccesNonAutoriseException(
                    "Seul l'intervenant assigné à cet entretien peut l'annuler");
        }

        if (!STATUTS_ACTIFS.contains(interview.getStatut())) {
            throw new TransitionStatutInvalideException("Seul un entretien planifié ou reporté peut être annulé");
        }

        interview.setStatut(InterviewStatus.ANNULE);
        interview.setNotes(motif != null ? motif : interview.getNotes());
        interview.setDateModification(LocalDateTime.now());
        interview.setActiveSlotKey(null);

        Interview saved = interviewRepository.save(interview);

        if (saved.getApplicationId() != null && saved.getType() != null) {
            Application application = applyService.getApplicationOuException(saved.getApplicationId());
            ApplicationStatus retour = statutRequisPourPlanifier(saved.getType());
            if (application.getStatut() != retour
                    && application.getStatut() != ApplicationStatus.RETIRE
                    && application.getStatut() != ApplicationStatus.REJETE) {
                applyService.changerStatutSysteme(saved.getApplicationId(), retour,
                        "Entretien " + libelle(saved.getType()) + " annulé"
                                + (motif != null ? " : " + motif : ""), auteurKeycloakId);
            }
        }

        return toDto(saved);
    }

    /**
     * BUG FIX : transition ABSENT absente. Un candidat absent est traité
     * comme un échec métier (rejet), avec une trace dédiée dans les notes.
     */
    @Transactional
    public InterviewDto marquerAbsent(String idInterview, String auteurKeycloakId, boolean isRH) {
        Interview interview = getEntityOuException(idInterview);

        if (!isRH
                && interview.getRecruteurKeycloakId() != null
                && !interview.getRecruteurKeycloakId().equals(auteurKeycloakId)) {
            throw new AccesNonAutoriseException(
                    "Seul l'intervenant assigné à cet entretien peut le marquer absent");
        }

        if (!STATUTS_ACTIFS.contains(interview.getStatut())) {
            throw new TransitionStatutInvalideException("Seul un entretien planifié ou reporté peut être marqué absent");
        }

        interview.setStatut(InterviewStatus.ABSENT);
        interview.setDateModification(LocalDateTime.now());
        interview.setActiveSlotKey(null);

        Interview saved = interviewRepository.save(interview);

        if (saved.getApplicationId() != null) {
            applyService.changerStatutSysteme(saved.getApplicationId(), ApplicationStatus.REJETE,
                    "Candidat absent à l'entretien " + libelle(saved.getType()), auteurKeycloakId);
        }

        return toDto(saved);
    }

    /**
     * BUG FIX : transition REPORTE absente. Reste actif (garde son
     * activeSlotKey), seule la date change.
     */
    @Transactional
    public InterviewDto reporterEntretien(
            String idInterview, LocalDateTime nouvelleDate, String auteurKeycloakId, boolean isRH) {

        Interview interview = getEntityOuException(idInterview);

        if (!isRH
                && interview.getRecruteurKeycloakId() != null
                && !interview.getRecruteurKeycloakId().equals(auteurKeycloakId)) {
            throw new AccesNonAutoriseException(
                    "Seul l'intervenant assigné à cet entretien peut le reporter");
        }

        if (!STATUTS_ACTIFS.contains(interview.getStatut())) {
            throw new TransitionStatutInvalideException("Seul un entretien planifié ou reporté peut être reporté");
        }
        if (nouvelleDate == null || !nouvelleDate.isAfter(LocalDateTime.now())) {
            throw new TransitionStatutInvalideException("La nouvelle date de l'entretien doit être dans le futur");
        }

        interview.setStatut(InterviewStatus.REPORTE);
        interview.setDateEntretien(nouvelleDate);
        interview.setDateFinEntretien(nouvelleDate.plusHours(1));
        interview.setDateModification(LocalDateTime.now());

        return toDto(interviewRepository.save(interview));
    }

    /**
     * BUG FIX (mélange d'entretiens entre cycles / créneau fantôme après
     * retrait) : appelé automatiquement quand ApplyService publie
     * ApplicationInterviewsShouldCloseEvent (retrait de candidature ou
     * redépôt après retrait). Ferme tout entretien encore actif rattaché à
     * cette candidature et libère son activeSlotKey, pour qu'un nouveau
     * cycle puisse planifier sereinement de nouveaux entretiens.
     */
    @EventListener
    @Transactional
    public void onApplicationInterviewsShouldClose(ApplicationInterviewsShouldCloseEvent event) {
        List<Interview> actifs = interviewRepository
                .findByApplicationIdOrderByDateCreationDesc(event.getApplicationId())
                .stream()
                .filter(i -> STATUTS_ACTIFS.contains(i.getStatut()))
                .toList();

        for (Interview interview : actifs) {
            interview.setStatut(InterviewStatus.ANNULE);
            interview.setActiveSlotKey(null);
            interview.setDateModification(LocalDateTime.now());
            String note = event.getMotif() != null ? event.getMotif() : "Clôturé automatiquement";
            interview.setNotes(note);
            interviewRepository.save(interview);
            log.info("Entretien {} clôturé automatiquement pour la candidature {} ({})",
                    interview.getIdInterview(), event.getApplicationId(), note);
        }
    }

    public List<InterviewDto> getEntretiensPourCandidature(String idApplication, String requesterKeycloakId, boolean isRhOuEmployee) {
        Application application = applyService.getApplicationOuException(idApplication);

        if (!isRhOuEmployee && !application.getCandidatKeycloakId().equals(requesterKeycloakId)) {
            throw new AccesNonAutoriseException("Accès non autorisé à cette candidature");
        }

        return interviewRepository.findByApplicationIdOrderByDateCreationDesc(idApplication)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private void envoyerConvocation(Application application, Interview interview) {
        if (application.getEmail() == null || application.getEmail().isBlank()) {
            log.warn("Email candidat vide, convocation non envoyée pour {}", application.getIdApplication());
            return;
        }
        try {
            PosteRecrutement poste = posteRecrutementService.getById(application.getPosteRecrutementId());
            recrutementMail.sendEntretienConvocation(poste, application, interview);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la convocation {}", interview.getIdInterview(), e);
        }
    }

    private ApplicationStatus statutRequisPourPlanifier(InterviewType type) {
        return switch (type) {
            case RH_INITIAL -> ApplicationStatus.SELECTIONNE;
            case TECHNIQUE -> ApplicationStatus.EN_ENTRETIEN_TECHNIQUE;
            case RH_FINAL -> ApplicationStatus.EN_ENTRETIEN_FINAL;
        };
    }

    private ApplicationStatus statutEnCoursPour(InterviewType type) {
        return switch (type) {
            case RH_INITIAL -> ApplicationStatus.EN_ENTRETIEN_RH;
            case TECHNIQUE -> ApplicationStatus.EN_ENTRETIEN_TECHNIQUE;
            case RH_FINAL -> ApplicationStatus.EN_ENTRETIEN_FINAL;
        };
    }

    private ApplicationStatus statutApresReussite(InterviewType type) {
        return switch (type) {
            case RH_INITIAL -> ApplicationStatus.EN_ENTRETIEN_TECHNIQUE;
            case TECHNIQUE -> ApplicationStatus.EN_ENTRETIEN_FINAL;
            case RH_FINAL -> ApplicationStatus.ACCEPTE;
        };
    }

    public static String libelle(InterviewType type) {
        if (type == null) return "libre";
        return switch (type) {
            case RH_INITIAL -> "RH initial";
            case TECHNIQUE -> "technique";
            case RH_FINAL -> "RH final";
        };
    }

    private static String calculerActiveSlotKey(String applicationId, InterviewType type, int cycle) {
        return applicationId + "|" + type + "|" + cycle;
    }

    /**
     * BUG FIX : on stockait auparavant l'UUID Keycloak brut comme
     * interviewerName. On tente ici de résoudre un nom affichable via
     * UserServiceClient.
     * <p>
     * ADAPTER : le nom exact de la méthode dépend de votre client existant
     * (ex: userServiceClient.getEmployeByKeycloakId(id)). Remplacez l'appel
     * ci-dessous par la méthode réelle exposée par votre UserServiceClient.
     * En attendant / en cas d'échec, on retombe sur le nom fourni
     * explicitement par l'appelant (fallbackName), puis sur l'UUID en tout
     * dernier recours, avec un log pour ne pas masquer le problème.
     */
    private String resolveDisplayName(String keycloakId, String fallbackName) {
        if (!estVide(fallbackName) && !estUuid(fallbackName)) {
            return fallbackName.trim();
        }

        String id = !estVide(keycloakId)
                ? keycloakId
                : (estUuid(fallbackName) ? fallbackName.trim() : null);

        if (!estVide(id)) {
            String cached = displayNameCache.get(id);
            if (!estVide(cached)) {
                return cached;
            }

            String fromDirectory = fetchDisplayNameFromDirectory(id);
            if (!estVide(fromDirectory)) {
                displayNameCache.put(id, fromDirectory);
                return fromDirectory;
            }

            String fromJwt = fetchDisplayNameFromJwt(id);
            if (!estVide(fromJwt)) {
                displayNameCache.put(id, fromJwt);
                return fromJwt;
            }
        }

        if (!estVide(fallbackName) && !estUuid(fallbackName)) {
            return fallbackName.trim();
        }
        // ne plus renvoyer l'UUID à l'UI
        return !estVide(fallbackName) && !estUuid(fallbackName) ? fallbackName : "—";
    }

    private String fetchDisplayNameFromDirectory(String keycloakId) {
        if (directoryNameMisses.contains(keycloakId)) {
            return null;
        }
        try {
            CandidatDto user = userServiceClient.getCandidatByKeycloakId(keycloakId);
            if (user != null) {
                String composed = composerNom(user.getPrenom(), user.getNom());
                if (!estVide(composed)) {
                    return composed;
                }
                if (!estVide(user.getEmail())) {
                    return user.getEmail().trim();
                }
            }
        } catch (Exception e) {
            log.debug("Annuaire users injoignable pour {} : {}", keycloakId, e.getMessage());
        }
        directoryNameMisses.add(keycloakId);
        return null;
    }

    private String fetchDisplayNameFromJwt(String keycloakId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return null;
            }
            Object principal = authentication.getPrincipal();
            if (!(principal instanceof Jwt jwt)) {
                return null;
            }
            if (!keycloakId.equals(jwt.getSubject())) {
                return null;
            }
            String composed = composerNom(
                    jwt.getClaimAsString("given_name"),
                    jwt.getClaimAsString("family_name"));
            if (estVide(composed)) {
                composed = composerNom(
                        jwt.getClaimAsString("prenom"),
                        jwt.getClaimAsString("nom"));
            }
            if (!estVide(composed)) {
                return composed;
            }
            if (!estVide(jwt.getClaimAsString("name"))) {
                return jwt.getClaimAsString("name").trim();
            }
            if (!estVide(jwt.getClaimAsString("preferred_username"))) {
                return jwt.getClaimAsString("preferred_username").trim();
            }
            if (!estVide(jwt.getClaimAsString("email"))) {
                return jwt.getClaimAsString("email").trim();
            }
        } catch (Exception e) {
            log.debug("Lecture du nom depuis le JWT impossible : {}", e.getMessage());
        }
        return null;
    }

    private boolean estUuid(String value) {
        return value != null
                && value.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    private String composerNom(String prenom, String nom) {
        String p = prenom == null ? "" : prenom.trim();
        String n = nom == null ? "" : nom.trim();
        String full = (p + " " + n).trim();
        return full.isEmpty() ? null : full;
    }
    // ==================== Mapping ====================

    private Interview getEntityOuException(String id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new CandidatureNotFoundException(id));
    }

    private InterviewDto toDto(Interview e) {
        LocalDate date = e.getDateEntretien() != null ? e.getDateEntretien().toLocalDate() : null;
        LocalTime start = e.getDateEntretien() != null ? e.getDateEntretien().toLocalTime() : null;
        LocalDateTime fin = e.getDateFinEntretien() != null ? e.getDateFinEntretien()
                : (e.getDateEntretien() != null ? e.getDateEntretien().plusHours(1) : null);

        return InterviewDto.builder()
                .id(e.getIdInterview())
                .source(e.getSource())
                .applicationId(e.getApplicationId())
                .candidatKeycloakId(e.getCandidatKeycloakId())
                .candidateName(e.getCandidateName())
                .candidateEmail(e.getCandidateEmail())
                .posteRecrutement(e.getPosteRecrutement())
                .posteId(e.getPosteId())
                .recruteurKeycloakId(e.getRecruteurKeycloakId())
                .interviewerName(e.getInterviewerName())
                .type(e.getType())
                .interviewDate(date != null ? date.toString() : null)
                .startTime(start != null ? start.format(TIME_FMT) : null)
                .endTime(fin != null ? fin.toLocalTime().format(TIME_FMT) : null)
                .mode(e.getMode())
                .location(e.getLieu())
                .meetingLink(e.getLienVisio())
                .status(e.getStatut())
                .resultat(e.getResultat())
                .notes(e.getNotes())
                .createdAt(e.getDateCreation() != null ? e.getDateCreation().toString() : null)
                .updatedAt(e.getDateModification() != null ? e.getDateModification().toString() : null)
                .build();
    }

    /**
     * BUG FIX : un LocalDate.parse / LocalTime.parse malformé venant du
     * front remontait auparavant en DateTimeParseException non gérée
     * (500 générique). On la traduit désormais en erreur métier claire.
     */
    private Interview fromDto(InterviewDto dto) {
        try {
            LocalDate date = dto.getInterviewDate() != null ? LocalDate.parse(dto.getInterviewDate()) : null;
            LocalTime start = dto.getStartTime() != null ? LocalTime.parse(dto.getStartTime(), TIME_FMT) : null;
            LocalTime end = dto.getEndTime() != null ? LocalTime.parse(dto.getEndTime(), TIME_FMT) : null;

            return Interview.builder()
                    .idInterview(dto.getId())
                    .candidatKeycloakId(dto.getCandidatKeycloakId())
                    .candidateName(dto.getCandidateName())
                    .candidateEmail(dto.getCandidateEmail())
                    .posteRecrutement(dto.getPosteRecrutement())
                    .posteId(dto.getPosteId())
                    .recruteurKeycloakId(dto.getRecruteurKeycloakId())
                    .interviewerName(dto.getInterviewerName())
                    .dateEntretien(date != null && start != null ? LocalDateTime.of(date, start) : null)
                    .dateFinEntretien(date != null && end != null ? LocalDateTime.of(date, end) : null)
                    .mode(dto.getMode())
                    .lieu(dto.getLocation())
                    .lienVisio(dto.getMeetingLink())
                    .statut(dto.getStatus())
                    .resultat(dto.getResultat())
                    .notes(dto.getNotes())
                    .build();
        } catch (DateTimeParseException e) {
            throw new TransitionStatutInvalideException(
                    "Date ou heure d'entretien invalide : " + e.getMessage());
        }
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }

    private String normaliser(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    public List<InterviewDto> getMesEntretiens(String keycloakId) {
        return interviewRepository.findByRecruteurKeycloakIdOrderByDateEntretienAsc(keycloakId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<CandidatEntretienTechniqueDto> getMesCandidatsTechniques(String employeeKeycloakId) {
        return interviewRepository
                .findByRecruteurKeycloakIdAndTypeAndSource(
                        employeeKeycloakId, InterviewType.TECHNIQUE, InterviewSource.CANDIDATURE)
                .stream()
                .filter(i -> STATUTS_ACTIFS.contains(i.getStatut()))
                .sorted(Comparator.comparing(Interview::getDateEntretien,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toCandidatTechniqueDto) // overload (Interview e) déjà présent
                .collect(Collectors.toList());
    }
}