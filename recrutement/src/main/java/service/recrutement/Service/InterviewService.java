package service.recrutement.Service;
import service.recrutement.Entity.Enum.CalendarInterviewMode;
import service.recrutement.Entity.Enum.CalendarInterviewStatus;
import service.recrutement.Entity.dto.CalendarInterviewDto;
import java.time.LocalDate; import java.time.LocalTime; import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.Enum.InterviewMode;
import service.recrutement.Entity.Enum.InterviewResult;
import service.recrutement.Entity.Enum.InterviewStatus;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.Interview;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.InterviewDto;
import service.recrutement.Entity.dto.PlanifierEntretienDto;
import service.recrutement.Entity.dto.ResultatEntretienDto;
import service.recrutement.Exception.AccesNonAutoriseException;
import service.recrutement.Exception.CandidatureNotFoundException;
import service.recrutement.Exception.TransitionStatutInvalideException;
import service.recrutement.Mail.RecrutementMail;
import service.recrutement.Repository.InterviewRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplyService applyService;
    private final PosteRecrutementService posteRecrutementService;
    private final RecrutementMail recrutementMail;
public List<CalendarInterviewDto> getAllRecrutementAsCalendar(){
   return interviewRepository.findAll().stream().map(i->{
     try{
       var app=applyService.getApplicationOuException(i.getApplicationId());
       var poste=posteRecrutementService.getById(app.getPosteRecrutementId());
       LocalDate date=i.getDateEntretien()!=null?i.getDateEntretien().toLocalDate():LocalDate.now();
       LocalTime start=i.getDateEntretien()!=null?i.getDateEntretien().toLocalTime():LocalTime.of(9,0);
       var mode=switch(i.getMode()){ case TELEPHONIQUE->CalendarInterviewMode.TELEPHONIQUE; case MEET->CalendarInterviewMode.DISTANCIEL; case PRESENTIEL->CalendarInterviewMode.PRESENTIEL; };
       var status=switch(i.getStatut()){ case PLANIFIE,REPLANIFIE->CalendarInterviewStatus.PLANIFIE; case TERMINE->CalendarInterviewStatus.TERMINE; case ANNULE->CalendarInterviewStatus.ANNULE; case ABSENT->CalendarInterviewStatus.ANNULE; };
       return CalendarInterviewDto.builder().id(i.getIdInterview()).candidateName(app.getNomComplet()!=null?app.getNomComplet():i.getCandidatKeycloakId()).candidateEmail(app.getEmail()).posteRecrutement(poste!=null?poste.getTitre():"Poste").posteId(app.getPosteRecrutementId()).interviewerName(i.getRecruteurKeycloakId()).interviewDate(date.toString()).startTime(start.format(DateTimeFormatter.ofPattern("HH:mm"))).endTime(start.plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm"))).mode(mode).location(i.getLieu()).meetingLink(i.getLienVisio()).status(status).notes(i.getNotes()).createdAt(i.getDateCreation()!=null?i.getDateCreation().toString():null).build();
     }catch(Exception e){ log.warn("skip {}",i.getIdInterview()); return null; }
   }).filter(o->o!=null).collect(Collectors.toList());
}
    @Transactional
    public InterviewDto planifierEntretien(
            String idApplication,
            InterviewType type,
            PlanifierEntretienDto dto,
            String auteurKeycloakId) {

        if (type == null) {
            throw new TransitionStatutInvalideException(
                    "Le type d'entretien est obligatoire"
            );
        }

        Application application =
                applyService.getApplicationOuException(idApplication);

        ApplicationStatus statutRequis =
                statutRequisPourPlanifier(type);

        if (application.getStatut() != statutRequis) {
            throw new TransitionStatutInvalideException(
                    "Impossible de planifier l'entretien "
                            + libelle(type)
                            + ". Statut actuel : "
                            + application.getStatut()
                            + ". Statut attendu : "
                            + statutRequis
            );
        }

        validerPlanification(type, dto);

        boolean dejaPlanifie =
                interviewRepository
                        .existsByApplicationIdAndTypeAndStatutIn(
                                idApplication,
                                type,
                                List.of(
                                        InterviewStatus.PLANIFIE,
                                        InterviewStatus.REPLANIFIE
                                )
                        );

        if (dejaPlanifie) {
            throw new TransitionStatutInvalideException(
                    "Un entretien "
                            + libelle(type)
                            + " est déjà planifié pour cette candidature"
            );
        }

        LocalDateTime maintenant = LocalDateTime.now();

        Interview interview = Interview.builder()
                .applicationId(idApplication)
                .candidatKeycloakId(
                        application.getCandidatKeycloakId()
                )
                .recruteurKeycloakId(auteurKeycloakId)
                .type(type)
                .mode(dto.getMode())
                .dateEntretien(dto.getDateEntretien())
                .lieu(normaliser(dto.getLieu()))
                .lienVisio(normaliser(dto.getLienVisio()))
                .statut(InterviewStatus.PLANIFIE)
                .dateCreation(maintenant)
                .dateModification(maintenant)
                .build();

        Interview saved = interviewRepository.save(interview);

        ApplicationStatus nouveauStatut =
                statutEnCoursPour(type);

        if (application.getStatut() != nouveauStatut) {
            applyService.changerStatutSysteme(
                    idApplication,
                    nouveauStatut,
                    "Entretien "
                            + libelle(type)
                            + " planifié",
                    auteurKeycloakId
            );
        }

        envoyerConvocation(application, saved);

        return toDto(saved);
    }

    private void validerPlanification(
            InterviewType type,
            PlanifierEntretienDto dto) {

        if (dto == null) {
            throw new TransitionStatutInvalideException(
                    "Les informations de l'entretien sont obligatoires"
            );
        }

        if (dto.getMode() == null) {
            throw new TransitionStatutInvalideException(
                    "Le mode de l'entretien est obligatoire"
            );
        }

        if (dto.getDateEntretien() == null) {
            throw new TransitionStatutInvalideException(
                    "La date de l'entretien est obligatoire"
            );
        }

        if (!dto.getDateEntretien().isAfter(LocalDateTime.now())) {
            throw new TransitionStatutInvalideException(
                    "La date de l'entretien doit être dans le futur"
            );
        }

        if (dto.getMode() == InterviewMode.TELEPHONIQUE
                && type != InterviewType.RH_INITIAL) {

            throw new TransitionStatutInvalideException(
                    "Le téléphone est autorisé uniquement pour l'entretien RH initial"
            );
        }

        if (dto.getMode() == InterviewMode.PRESENTIEL
                && estVide(dto.getLieu())) {

            throw new TransitionStatutInvalideException(
                    "Le lieu est obligatoire pour un entretien présentiel"
            );
        }

        if (dto.getMode() == InterviewMode.MEET
                && estVide(dto.getLienVisio())) {

            throw new TransitionStatutInvalideException(
                    "Le lien Google Meet est obligatoire"
            );
        }
    }

    @Transactional
    public InterviewDto enregistrerResultat(
            String idInterview,
            ResultatEntretienDto dto,
            String auteurKeycloakId) {

        if (dto == null || dto.getResultat() == null) {
            throw new TransitionStatutInvalideException(
                    "Le résultat de l'entretien est obligatoire"
            );
        }

        Interview interview =
                interviewRepository.findById(idInterview)
                        .orElseThrow(() ->
                                new CandidatureNotFoundException(idInterview)
                        );

        if (interview.getStatut() != InterviewStatus.PLANIFIE
                && interview.getStatut() != InterviewStatus.REPLANIFIE) {

            throw new TransitionStatutInvalideException(
                    "Cet entretien ne peut plus recevoir de résultat"
            );
        }

        if (dto.getResultat() == InterviewResult.ECHOUE
                && estVide(dto.getNotes())) {

            throw new TransitionStatutInvalideException(
                    "Une note est obligatoire lorsqu'un entretien est échoué"
            );
        }

        interview.setStatut(InterviewStatus.TERMINE);
        interview.setResultat(dto.getResultat());
        interview.setNotes(normaliser(dto.getNotes()));
        interview.setDateModification(LocalDateTime.now());

        Interview saved = interviewRepository.save(interview);

        if (dto.getResultat() == InterviewResult.ECHOUE) {

            String commentaire =
                    "Entretien "
                            + libelle(interview.getType())
                            + " non concluant";

            if (!estVide(dto.getNotes())) {
                commentaire += " : " + dto.getNotes().trim();
            }

            applyService.changerStatutSysteme(
                    interview.getApplicationId(),
                    ApplicationStatus.REJETE,
                    commentaire,
                    auteurKeycloakId
            );

        } else {

            ApplicationStatus prochainStatut =
                    statutApresReussite(interview.getType());

            applyService.changerStatutSysteme(
                    interview.getApplicationId(),
                    prochainStatut,
                    "Entretien "
                            + libelle(interview.getType())
                            + " réussi",
                    auteurKeycloakId
            );
        }

        return toDto(saved);
    }

    public List<InterviewDto> getEntretiensPourCandidature(
            String idApplication,
            String requesterKeycloakId,
            boolean isRhOuEmployee) {

        Application application =
                applyService.getApplicationOuException(idApplication);

        if (!isRhOuEmployee
                && !application.getCandidatKeycloakId()
                .equals(requesterKeycloakId)) {

            throw new AccesNonAutoriseException(
                    "Accès non autorisé à cette candidature"
            );
        }

        return interviewRepository
                .findByApplicationIdOrderByDateCreationDesc(idApplication)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void envoyerConvocation(
            Application application,
            Interview interview) {

        if (application.getEmail() == null
                || application.getEmail().isBlank()) {

            log.warn(
                    "Email candidat vide, convocation non envoyée pour {}",
                    application.getIdApplication()
            );

            return;
        }

        try {
            PosteRecrutement poste =
                    posteRecrutementService.getById(
                            application.getPosteRecrutementId()
                    );

            recrutementMail.sendEntretienConvocation(
                    poste,
                    application,
                    interview
            );

        } catch (Exception e) {
            log.error(
                    "Erreur lors de l'envoi de la convocation {}",
                    interview.getIdInterview(),
                    e
            );
        }
    }

    private ApplicationStatus statutRequisPourPlanifier(
            InterviewType type) {

        return switch (type) {
            case RH_INITIAL ->
                    ApplicationStatus.SELECTIONNE;

            case TECHNIQUE ->
                    ApplicationStatus.EN_ENTRETIEN_TECHNIQUE;

            case RH_FINAL ->
                    ApplicationStatus.EN_ENTRETIEN_FINAL;
        };
    }

    private ApplicationStatus statutEnCoursPour(
            InterviewType type) {

        return switch (type) {
            case RH_INITIAL ->
                    ApplicationStatus.EN_ENTRETIEN_RH;

            case TECHNIQUE ->
                    ApplicationStatus.EN_ENTRETIEN_TECHNIQUE;

            case RH_FINAL ->
                    ApplicationStatus.EN_ENTRETIEN_FINAL;
        };
    }

    private ApplicationStatus statutApresReussite(
            InterviewType type) {

        return switch (type) {
            case RH_INITIAL ->
                    ApplicationStatus.EN_ENTRETIEN_TECHNIQUE;

            case TECHNIQUE ->
                    ApplicationStatus.EN_ENTRETIEN_FINAL;

            case RH_FINAL ->
                    ApplicationStatus.ACCEPTE;
        };
    }

    public static String libelle(InterviewType type) {
        return switch (type) {
            case RH_INITIAL -> "RH initial";
            case TECHNIQUE -> "technique";
            case RH_FINAL -> "RH final";
        };
    }

    private InterviewDto toDto(Interview interview) {
        return InterviewDto.builder()
                .idInterview(interview.getIdInterview())
                .applicationId(interview.getApplicationId())
                .mode(interview.getMode())
                .type(interview.getType())
                .dateEntretien(interview.getDateEntretien())
                .lieu(interview.getLieu())
                .lienVisio(interview.getLienVisio())
                .notes(interview.getNotes())
                .statut(interview.getStatut())
                .resultat(interview.getResultat())
                .dateCreation(interview.getDateCreation())
                .dateModification(interview.getDateModification())
                .build();
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }

    private String normaliser(String valeur) {
        return valeur == null || valeur.isBlank()
                ? null
                : valeur.trim();
    }
}