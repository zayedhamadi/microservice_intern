package service.recrutement.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.*;
import service.recrutement.Entity.Interview;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.InterviewStatsDto;
import service.recrutement.Entity.dto.InterviewDto;
import service.recrutement.Entity.dto.PlanifierEntretienDto;
import service.recrutement.Entity.dto.ResultatEntretienDto;
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
import java.util.*;
import java.util.stream.Collectors;

import service.recrutement.Entity.dto.CandidatEntretienTechniqueDto;
import service.recrutement.Entity.dto.PosteEntretiensTechniquesDto;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final ApplyService applyService;
    private final PosteRecrutementService posteRecrutementService;
    private final RecrutementMail recrutementMail;

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
        interview.setRecruteurKeycloakId(auteurKeycloakId); // jamais depuis le dto client
        interview.setDateCreation(now);
        interview.setDateModification(now);

        return toDto(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewDto updateLibre(String id, InterviewDto dto, String auteurKeycloakId) {
        Interview existant = getEntityOuException(id);
        validerDtoLibre(dto);

        Interview maj = fromDto(dto);
        maj.setIdInterview(existant.getIdInterview());
        maj.setSource(existant.getSource());
        maj.setApplicationId(existant.getApplicationId());
        maj.setType(existant.getType());
        maj.setCandidatKeycloakId(existant.getCandidatKeycloakId());
        // si l'entretien n'a pas encore de recruteur (ex: ancien entretien candidature),
        // on l'attribue à celui qui modifie ; sinon on conserve le propriétaire d'origine
        maj.setRecruteurKeycloakId(
                existant.getRecruteurKeycloakId() != null ? existant.getRecruteurKeycloakId() : auteurKeycloakId);
        maj.setDateCreation(existant.getDateCreation());
        maj.setDateModification(LocalDateTime.now());

        return toDto(interviewRepository.save(maj));
    }

    @Transactional
    public void delete(String id) {
        getEntityOuException(id); // vérifie que l'entretien existe
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

        // 2) Interviews techniques déjà planifiées, indexées par applicationId
        //    (peut ne pas exister si personne n'a encore cliqué "Planifier")
        Map<String, Interview> interviewParApplication = interviewRepository
                .findByTypeAndSource(InterviewType.TECHNIQUE, InterviewSource.CANDIDATURE)
                .stream()
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

        boolean dejaPlanifie = interviewRepository.existsByApplicationIdAndTypeAndStatutIn(
                idApplication, type, List.of(InterviewStatus.PLANIFIE, InterviewStatus.REPORTE));

        if (dejaPlanifie) {
            throw new TransitionStatutInvalideException(
                    "Un entretien " + libelle(type) + " est déjà planifié pour cette candidature");
        }

        PosteRecrutement poste = posteRecrutementService.getById(application.getPosteRecrutementId());

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
                .interviewerName(auteurKeycloakId)
                .type(type)
                .mode(dto.getMode())
                .dateEntretien(dto.getDateEntretien())
                .dateFinEntretien(dto.getDateEntretien().plusHours(1))
                .lieu(normaliser(dto.getLieu()))
                .lienVisio(normaliser(dto.getLienVisio()))
                .statut(InterviewStatus.PLANIFIE)
                .dateCreation(maintenant)
                .dateModification(maintenant)
                .build();

        Interview saved = interviewRepository.save(interview);

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
        if (dto.getMode() == InterviewMode.DISTANCIEL && estVide(dto.getLienVisio())) {
            throw new TransitionStatutInvalideException("Le lien de visioconférence est obligatoire");
        }
    }

    @Transactional
    public InterviewDto enregistrerResultat(String idInterview, ResultatEntretienDto dto, String auteurKeycloakId) {
        if (dto == null || dto.getResultat() == null) {
            throw new TransitionStatutInvalideException("Le résultat de l'entretien est obligatoire");
        }

        Interview interview = getEntityOuException(idInterview);

        if (interview.getStatut() != InterviewStatus.PLANIFIE && interview.getStatut() != InterviewStatus.REPORTE) {
            throw new TransitionStatutInvalideException("Cet entretien ne peut plus recevoir de résultat");
        }

        if (dto.getResultat() == InterviewResult.ECHOUE && estVide(dto.getNotes())) {
            throw new TransitionStatutInvalideException("Une note est obligatoire lorsqu'un entretien est échoué");
        }

        interview.setStatut(InterviewStatus.TERMINE);
        interview.setResultat(dto.getResultat());
        interview.setNotes(normaliser(dto.getNotes()));
        interview.setDateModification(LocalDateTime.now());

        Interview saved = interviewRepository.save(interview);

        if (saved.getApplicationId() != null) {
            if (dto.getResultat() == InterviewResult.ECHOUE) {
                String commentaire = "Entretien " + libelle(saved.getType()) + " non concluant";
                if (!estVide(dto.getNotes())) commentaire += " : " + dto.getNotes().trim();
                applyService.changerStatutSysteme(saved.getApplicationId(), ApplicationStatus.REJETE, commentaire, auteurKeycloakId);
            } else {
                ApplicationStatus prochainStatut = statutApresReussite(saved.getType());
                applyService.changerStatutSysteme(saved.getApplicationId(), prochainStatut,
                        "Entretien " + libelle(saved.getType()) + " réussi", auteurKeycloakId);
            }
        }

        return toDto(saved);
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

    private Interview fromDto(InterviewDto dto) {
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
                .sorted(Comparator.comparing(Interview::getDateEntretien,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toCandidatTechniqueDto) // overload (Interview e) déjà présent
                .collect(Collectors.toList());
    }
}