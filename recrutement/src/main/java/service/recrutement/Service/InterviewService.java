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
import service.recrutement.WebSocket.RecrutementRealtimeService;

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
    private final RecrutementRealtimeService recrutementRealtimeService;

    private static final Set<InterviewStatus> STATUTS_ACTIFS =
            EnumSet.of(InterviewStatus.PLANIFIE, InterviewStatus.REPORTE);

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

        if (dto.getMode() == InterviewMode.DISTANCIEL) {
            String genere = googleMeetService.genererLienMeet(
                    "Entretien - " + dto.getCandidateName(),
                    dto.getCandidateEmail(),
                    null,
                    interview.getDateEntretien()
            );
            interview.setLienVisio(normaliser(genere != null ? genere : dto.getMeetingLink()));
        }

        interview.setDateCreation(now);
        interview.setDateModification(now);

        Interview saved = interviewRepository.save(interview);
        recrutementRealtimeService.notifyInterviewPlanifie(saved);

        try {
            recrutementMail.sendEntretienConvocationLibre(saved);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la convocation pour l'entretien libre {}", saved.getIdInterview(), e);
        }

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
            maj.setLienVisio(existant.getLienVisio());
        }

        maj.setDateCreation(existant.getDateCreation());
        maj.setDateModification(LocalDateTime.now());

        Interview saved = interviewRepository.save(maj);
        recrutementRealtimeService.notifyInterviewPlanifie(saved);

        try {
            recrutementMail.sendEntretienConvocationLibre(saved);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la convocation (mise à jour) pour l'entretien libre {}", saved.getIdInterview(), e);
        }

        return toDto(saved);
    }

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

    public List<PosteEntretiensTechniquesDto> getEntretiensTechniquesParPoste() {
        List<Application> candidatures = applicationRepository
                .findByStatut(ApplicationStatus.EN_ENTRETIEN_TECHNIQUE);

        if (candidatures.isEmpty()) return List.of();

        Map<String, Interview> interviewParApplication = interviewRepository
                .findByTypeAndSource(InterviewType.TECHNIQUE, InterviewSource.CANDIDATURE)
                .stream()
                .filter(i -> STATUTS_ACTIFS.contains(i.getStatut()))
                .collect(Collectors.toMap(
                        Interview::getApplicationId,
                        i -> i,
                        (a, b) -> a.getDateCreation().isAfter(b.getDateCreation()) ? a : b
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
            return CandidatEntretienTechniqueDto.builder()
                    .applicationId(app.getIdApplication())
                    .candidatKeycloakId(app.getCandidatKeycloakId())
                    .candidateName(app.getNomComplet())
                    .candidateEmail(app.getEmail())
                    .status(InterviewStatus.PLANIFIE)
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

        boolean dejaPlanifie = interviewRepository.existsByApplicationIdAndTypeAndStatutIn(
                idApplication, type, List.of(InterviewStatus.PLANIFIE, InterviewStatus.REPORTE));

        if (dejaPlanifie) {
            throw new TransitionStatutInvalideException(
                    "Un entretien " + libelle(type) + " est déjà planifié pour cette candidature");
        }

        PosteRecrutement poste = posteRecrutementService.getById(application.getPosteRecrutementId());

        String lienVisioFinal = dto.getLienVisio();
        if (dto.getMode() == InterviewMode.DISTANCIEL) {
            String genere = googleMeetService.genererLienMeet(
                    "Entretien " + libelle(type) + " - " + application.getNomComplet(),
                    application.getEmail(),
                    null,
                    dto.getDateEntretien()
            );
            lienVisioFinal = (genere != null) ? genere : dto.getLienVisio();
        }

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
                .interviewerName(resolveDisplayName(auteurKeycloakId, null))
                .type(type)
                .mode(dto.getMode())
                .dateEntretien(dto.getDateEntretien())
                .dateFinEntretien(dto.getDateEntretien().plusHours(1))
                .lieu(normaliser(dto.getLieu()))
                .lienVisio(normaliser(lienVisioFinal))
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
            throw new TransitionStatutInvalideException(
                    "Un entretien " + libelle(type) + " est déjà en cours de planification pour cette candidature");
        }

        recrutementRealtimeService.notifyInterviewPlanifie(saved);

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
    }

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

        if (interview.getType() == InterviewType.RH_INITIAL
                || interview.getType() == InterviewType.RH_FINAL) {

            if (!isRH) {
                throw new AccesNonAutoriseException(
                        "Seul un RH peut enregistrer le résultat de cet entretien");
            }

        } else if (interview.getType() == InterviewType.TECHNIQUE) {

            if (interview.getRecruteurKeycloakId() == null
                    || !interview.getRecruteurKeycloakId().equals(auteurKeycloakId)) {

                throw new AccesNonAutoriseException(
                        "Seul l'employé assigné à cet entretien technique peut enregistrer le résultat");
            }
        }

        if (interview.getStatut() != InterviewStatus.PLANIFIE
                && interview.getStatut() != InterviewStatus.REPORTE) {

            throw new TransitionStatutInvalideException(
                    "Cet entretien ne peut plus recevoir de résultat");
        }

        if (dto.getResultat() == InterviewResult.ECHOUE
                && estVide(dto.getNotes())) {

            throw new TransitionStatutInvalideException(
                    "Une note est obligatoire lorsqu'un entretien est échoué");
        }

        interview.setStatut(InterviewStatus.TERMINE);
        interview.setResultat(dto.getResultat());
        interview.setNotes(normaliser(dto.getNotes()));
        interview.setDateModification(LocalDateTime.now());
        interview.setActiveSlotKey(null);

        Interview saved = interviewRepository.save(interview);
        recrutementRealtimeService.notifyInterviewResultat(saved);

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
        recrutementRealtimeService.notifyInterviewAnnule(saved);

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
        recrutementRealtimeService.notifyInterviewAbsent(saved);

        if (saved.getApplicationId() != null) {
            applyService.changerStatutSysteme(saved.getApplicationId(), ApplicationStatus.REJETE,
                    "Candidat absent à l'entretien " + libelle(saved.getType()), auteurKeycloakId);
        }

        return toDto(saved);
    }

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

        Interview saved = interviewRepository.save(interview);
        recrutementRealtimeService.notifyInterviewReporte(saved);
        return toDto(saved);
    }

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
            Interview saved = interviewRepository.save(interview);
            recrutementRealtimeService.notifyInterviewAnnule(saved);
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
                .map(this::toCandidatTechniqueDto)
                .collect(Collectors.toList());
    }
}