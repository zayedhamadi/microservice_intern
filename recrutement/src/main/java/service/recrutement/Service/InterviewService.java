package service.recrutement.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.*;
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

    @Transactional
    public InterviewDto planifierEntretien(
            String idApplication, InterviewType type, PlanifierEntretienDto dto, String auteurKeycloakId) {

        Application application = applyService.getApplicationOuException(idApplication);

        ApplicationStatus statutRequis = statutRequisPourPlanifier(type);
        if (application.getStatut() != statutRequis) {
            throw new TransitionStatutInvalideException(
                    "Impossible de planifier l'entretien " + libelle(type) + " : la candidature est au statut "
                            + application.getStatut() + " (attendu : " + statutRequis + ")");
        }

        validerMode(type, dto.getMode());

        Interview interview = Interview.builder()
                .applicationId(idApplication)
                .candidatKeycloakId(application.getCandidatKeycloakId())
                .recruteurKeycloakId(auteurKeycloakId)
                .type(type)
                .mode(dto.getMode())
                .dateEntretien(dto.getDateEntretien())
                .lieu(dto.getLieu())
                .lienVisio(dto.getLienVisio())
                .statut(InterviewStatus.PLANIFIE)
                .dateCreation(LocalDateTime.now())
                .dateModification(LocalDateTime.now())
                .build();

        Interview saved = interviewRepository.save(interview);

        ApplicationStatus statutCible = statutEnCoursPour(type);
        if (application.getStatut() != statutCible) {
            applyService.changerStatutSysteme(idApplication, statutCible,
                    "Entretien " + libelle(type) + " planifié", auteurKeycloakId);
        }

        envoyerConvocation(application, saved);

        return toDto(saved);
    }

    /**
     * Seul l'entretien RH initial peut se faire par téléphone. L'entretien
     * technique et l'entretien RH final doivent se dérouler en visio (Meet)
     * ou en présentiel — jamais par téléphone.
     */
    private void validerMode(InterviewType type, InterviewMode mode) {
        if (mode == null) {
            throw new TransitionStatutInvalideException("Le mode de l'entretien (téléphone / meet / présentiel) est requis");
        }
        if (mode == InterviewMode.TELEPHONIQUE && type != InterviewType.RH_INITIAL) {
            throw new TransitionStatutInvalideException(
                    "L'entretien " + libelle(type) + " doit se dérouler en visio (Meet) ou en présentiel — "
                            + "le téléphone n'est autorisé que pour l'entretien RH initial.");
        }
    }

    @Transactional
    public InterviewDto enregistrerResultat(String idInterview, ResultatEntretienDto dto, String auteurKeycloakId) {

        Interview interview = interviewRepository.findById(idInterview)
                .orElseThrow(() -> new CandidatureNotFoundException(idInterview));

        interview.setStatut(InterviewStatus.TERMINE);
        interview.setResultat(dto.getResultat());
        if (dto.getNotes() != null) {
            interview.setNotes(dto.getNotes());
        }
        interview.setDateModification(LocalDateTime.now());
        Interview saved = interviewRepository.save(interview);

        if (dto.getResultat() == InterviewResult.ECHOUE) {
            applyService.changerStatutSysteme(interview.getApplicationId(), ApplicationStatus.REJETE,
                    "Entretien " + libelle(interview.getType()) + " non concluant"
                            + (dto.getNotes() != null && !dto.getNotes().isBlank() ? " : " + dto.getNotes() : ""),
                    auteurKeycloakId);
        } else {
            ApplicationStatus prochainStatut = statutApresReussite(interview.getType());
            applyService.changerStatutSysteme(interview.getApplicationId(), prochainStatut,
                    "Entretien " + libelle(interview.getType()) + " réussi", auteurKeycloakId);
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
        try {
            if (application.getEmail() == null) return;
            PosteRecrutement poste = posteRecrutementService.getById(application.getPosteRecrutementId());
            recrutementMail.sendEntretienConvocation(poste, application, interview);
        } catch (Exception e) {
            log.warn("Convocation échouée pour l'entretien {} : {}", interview.getIdInterview(), e.getMessage());
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
        return switch (type) {
            case RH_INITIAL -> "RH";
            case TECHNIQUE -> "technique";
            case RH_FINAL -> "RH final";
        };
    }

    private InterviewDto toDto(Interview i) {
        return InterviewDto.builder()
                .mode(i.getMode())
                .idInterview(i.getIdInterview())
                .applicationId(i.getApplicationId())
                .type(i.getType())
                .dateEntretien(i.getDateEntretien())
                .lieu(i.getLieu())
                .lienVisio(i.getLienVisio())
                .notes(i.getNotes())
                .statut(i.getStatut())
                .resultat(i.getResultat())
                .dateCreation(i.getDateCreation())
                .dateModification(i.getDateModification())
                .build();
    }
}