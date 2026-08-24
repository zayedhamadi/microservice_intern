package service.recrutement.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.recrutement.Entity.Enum.*;
import service.recrutement.Entity.Interview;
import service.recrutement.Entity.Reprogrammer;
import service.recrutement.Entity.dto.DemanderReportDto;
import service.recrutement.Entity.dto.ReprogrammerDto;
import service.recrutement.Exception.AccesNonAutoriseException;
import service.recrutement.Exception.DemandeReportNotFoundException;
import service.recrutement.Exception.TransitionStatutInvalideException;
import service.recrutement.Repository.InterviewRepository;
import service.recrutement.Repository.ReprogrammerRepository;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReprogrammerService {

    private final ReprogrammerRepository reprogrammerRepository;
    private final InterviewRepository interviewRepository;
    private final ApplyService applyService;

    private static final Set<InterviewStatus> STATUTS_ACTIFS =
            EnumSet.of(InterviewStatus.PLANIFIE, InterviewStatus.REPORTE);

    // ==================== CRÉATION DES DEMANDES ====================

    /**
     * Le candidat demande à reporter un entretien actif qui le concerne.
     * Cible = intervenant assigné, sinon n'importe quel RH.
     */
    @Transactional
    public ReprogrammerDto demanderParCandidat(String interviewId, DemanderReportDto dto, String candidatKeycloakId) {
        Interview interview = getInterviewOuException(interviewId);

        if (!candidatKeycloakId.equals(interview.getCandidatKeycloakId())) {
            throw new AccesNonAutoriseException("Cet entretien ne concerne pas ce candidat");
        }
        if (!STATUTS_ACTIFS.contains(interview.getStatut())) {
            throw new TransitionStatutInvalideException(
                    "Seul un entretien planifié ou reporté peut faire l'objet d'une demande de report");
        }

        return creerDemande(interview, dto, TypeDemandeReport.DEMANDE_CANDIDAT,
                candidatKeycloakId, interview.getRecruteurKeycloakId());
    }

    /**
     * L'intervenant (RH pour RH_INITIAL/RH_FINAL, EMPLOYEE assigné pour
     * TECHNIQUE) propose un report au candidat, avec motif (charge, imprévu...).
     * Cible = candidat.
     */
    @Transactional
    public ReprogrammerDto proposerParIntervenant(
            String interviewId, DemanderReportDto dto, String auteurKeycloakId, boolean isRH) {

        Interview interview = getInterviewOuException(interviewId);
        verifierDroitSurEntretien(interview, auteurKeycloakId, isRH);

        if (!STATUTS_ACTIFS.contains(interview.getStatut())) {
            throw new TransitionStatutInvalideException(
                    "Seul un entretien planifié ou reporté peut faire l'objet d'une proposition de report");
        }

        return creerDemande(interview, dto, TypeDemandeReport.PROPOSITION_INTERVENANT,
                auteurKeycloakId, interview.getCandidatKeycloakId());
    }

    /**
     * Candidat marqué absent : demande de réactivation avec motif justificatif.
     * Cible = toujours un RH (décision humaine obligatoire, jamais automatique).
     */
    @Transactional
    public ReprogrammerDto demanderReactivationApresAbsence(
            String interviewId, DemanderReportDto dto, String candidatKeycloakId) {

        Interview interview = getInterviewOuException(interviewId);

        if (!candidatKeycloakId.equals(interview.getCandidatKeycloakId())) {
            throw new AccesNonAutoriseException("Cet entretien ne concerne pas ce candidat");
        }
        if (interview.getStatut() != InterviewStatus.ABSENT) {
            throw new TransitionStatutInvalideException(
                    "Une réactivation ne peut être demandée que pour un entretien marqué absent");
        }

        return creerDemande(interview, dto, TypeDemandeReport.REACTIVATION_APRES_ABSENCE,
                candidatKeycloakId, null);
    }

    private ReprogrammerDto creerDemande(
            Interview interview, DemanderReportDto dto, TypeDemandeReport type,
            String demandeurKeycloakId, String cibleKeycloakId) {

        if (dto == null || dto.getMotif() == null || dto.getMotif().isBlank()) {
            throw new TransitionStatutInvalideException("Le motif du report est obligatoire");
        }
        if (dto.getNouvelleDateProposee() == null || !dto.getNouvelleDateProposee().isAfter(LocalDateTime.now())) {
            throw new TransitionStatutInvalideException("La nouvelle date proposée doit être dans le futur");
        }
        // Évite la confusion : une seule demande EN_ATTENTE à la fois par entretien
        if (reprogrammerRepository.existsByInterviewIdAndStatut(interview.getIdInterview(), DemandeReportStatus.EN_ATTENTE)) {
            throw new TransitionStatutInvalideException(
                    "Une demande de report est déjà en attente pour cet entretien");
        }

        Reprogrammer demande = Reprogrammer.builder()
                .interviewId(interview.getIdInterview())
                .applicationId(interview.getApplicationId())
                .type(type)
                .demandeurKeycloakId(demandeurKeycloakId)
                .cibleKeycloakId(cibleKeycloakId)
                .ancienneDate(interview.getDateEntretien())
                .nouvelleDateProposee(dto.getNouvelleDateProposee())
                .motif(dto.getMotif().trim())
                .statut(DemandeReportStatus.EN_ATTENTE)
                .dateCreation(LocalDateTime.now())
                .build();

        return toDto(reprogrammerRepository.save(demande), interview);
    }

    // ==================== TRAITEMENT DES DEMANDES ====================

    @Transactional
    public ReprogrammerDto accepterDemande(String demandeId, String auteurKeycloakId, boolean isRH) {
        Reprogrammer demande = getDemandeOuException(demandeId);
        Interview interview = getInterviewOuException(demande.getInterviewId());

        verifierDroitDeTraitement(demande, auteurKeycloakId, isRH);
        verifierDemandeEnAttente(demande);

        if (demande.getType() == TypeDemandeReport.REACTIVATION_APRES_ABSENCE) {
            appliquerReactivation(demande, interview, auteurKeycloakId);
        } else {
            appliquerReport(demande, interview);
        }

        demande.setStatut(DemandeReportStatus.ACCEPTEE);
        demande.setTraiteParKeycloakId(auteurKeycloakId);
        demande.setDateTraitement(LocalDateTime.now());

        return toDto(reprogrammerRepository.save(demande), interview);
    }

    @Transactional
    public ReprogrammerDto refuserDemande(
            String demandeId, String commentaire, String auteurKeycloakId, boolean isRH) {

        if (commentaire == null || commentaire.isBlank()) {
            throw new TransitionStatutInvalideException(
                    "Un commentaire est obligatoire pour refuser une demande de report");
        }

        Reprogrammer demande = getDemandeOuException(demandeId);
        Interview interview = getInterviewOuException(demande.getInterviewId());

        verifierDroitDeTraitement(demande, auteurKeycloakId, isRH);
        verifierDemandeEnAttente(demande);

        demande.setStatut(DemandeReportStatus.REFUSEE);
        demande.setCommentaireTraitement(commentaire.trim());
        demande.setTraiteParKeycloakId(auteurKeycloakId);
        demande.setDateTraitement(LocalDateTime.now());

        return toDto(reprogrammerRepository.save(demande), interview);
    }

    private void appliquerReport(Reprogrammer demande, Interview interview) {
        if (!STATUTS_ACTIFS.contains(interview.getStatut())) {
            throw new TransitionStatutInvalideException(
                    "Cet entretien n'est plus actif, la demande de report ne peut plus être appliquée");
        }
        interview.setStatut(InterviewStatus.REPORTE);
        interview.setDateEntretien(demande.getNouvelleDateProposee());
        interview.setDateFinEntretien(demande.getNouvelleDateProposee().plusHours(1));
        interview.setDateModification(LocalDateTime.now());
        interviewRepository.save(interview);
    }

    /**
     * BUG évité : Application.statut = REJETE après une absence, et REJETE
     * n'a normalement aucune transition sortante. On passe par
     * ApplyService#reactiverApresAbsence, chemin dédié et encadré, déclenché
     * uniquement ici, après acceptation RH.
     */
    private void appliquerReactivation(Reprogrammer demande, Interview interview, String auteurKeycloakId) {
        if (interview.getStatut() != InterviewStatus.ABSENT) {
            throw new TransitionStatutInvalideException(
                    "Cet entretien n'est plus au statut absent, la réactivation ne peut plus être appliquée");
        }

        int cycle = interview.getCycleCandidature() != null ? interview.getCycleCandidature() : 1;
        String slotKey = interview.getApplicationId() + "|" + interview.getType() + "|" + cycle;

        interview.setStatut(InterviewStatus.PLANIFIE);
        interview.setDateEntretien(demande.getNouvelleDateProposee());
        interview.setDateFinEntretien(demande.getNouvelleDateProposee().plusHours(1));
        // BUG évité : si un nouvel entretien du même type a déjà été replanifié
        // entre-temps sur ce cycle, l'index unique sparse activeSlotKey empêchera
        // la sauvegarde (DuplicateKeyException) plutôt que de créer un doublon silencieux.
        interview.setActiveSlotKey(slotKey);
        interview.setDateModification(LocalDateTime.now());
        interviewRepository.save(interview);

        if (interview.getApplicationId() != null && interview.getType() != null) {
            ApplicationStatus statutCible = statutEnCoursPour(interview.getType());
            applyService.reactiverApresAbsence(
                    interview.getApplicationId(), statutCible,
                    "Entretien " + interview.getType() + " réactivé après absence justifiée : " + demande.getMotif(),
                    auteurKeycloakId);
        }
    }

    private ApplicationStatus statutEnCoursPour(InterviewType type) {
        return switch (type) {
            case RH_INITIAL -> ApplicationStatus.EN_ENTRETIEN_RH;
            case TECHNIQUE -> ApplicationStatus.EN_ENTRETIEN_TECHNIQUE;
            case RH_FINAL -> ApplicationStatus.EN_ENTRETIEN_FINAL;
        };
    }

    // ==================== AUTORISATIONS ====================

    private void verifierDroitSurEntretien(Interview interview, String auteurKeycloakId, boolean isRH) {
        boolean estRhOuFinal = interview.getType() == InterviewType.RH_INITIAL
                || interview.getType() == InterviewType.RH_FINAL;

        if (estRhOuFinal) {
            if (!isRH) {
                throw new AccesNonAutoriseException("Seul un RH peut proposer un report pour cet entretien");
            }
        } else {
            boolean estIntervenantAssigne = interview.getRecruteurKeycloakId() != null
                    && interview.getRecruteurKeycloakId().equals(auteurKeycloakId);
            if (!isRH && !estIntervenantAssigne) {
                throw new AccesNonAutoriseException(
                        "Seul l'employé assigné à cet entretien technique peut proposer un report");
            }
        }
    }

    private void verifierDroitDeTraitement(Reprogrammer demande, String auteurKeycloakId, boolean isRH) {
        // Un RH peut toujours arbitrer une demande, peu importe son type.
        if (isRH) return;

        if (demande.getCibleKeycloakId() != null) {
            if (!demande.getCibleKeycloakId().equals(auteurKeycloakId)) {
                throw new AccesNonAutoriseException("Cette demande de report ne vous est pas destinée");
            }
            return;
        }
        // cibleKeycloakId nul = réactivation après absence, réservée au RH.
        throw new AccesNonAutoriseException("Seul un RH peut traiter cette demande");
    }

    private void verifierDemandeEnAttente(Reprogrammer demande) {
        if (demande.getStatut() != DemandeReportStatus.EN_ATTENTE) {
            throw new TransitionStatutInvalideException("Cette demande a déjà été traitée");
        }
    }

    // ==================== LECTURE ====================

    public List<ReprogrammerDto> getDemandesPourEntretien(String interviewId, String requesterKeycloakId, boolean isRhOuEmployee) {
        Interview interview = getInterviewOuException(interviewId);

        if (!isRhOuEmployee && !requesterKeycloakId.equals(interview.getCandidatKeycloakId())) {
            throw new AccesNonAutoriseException("Accès non autorisé à cet entretien");
        }

        return reprogrammerRepository.findByInterviewIdOrderByDateCreationDesc(interviewId)
                .stream().map(d -> toDto(d, interview)).collect(Collectors.toList());
    }

    public List<ReprogrammerDto> getMesDemandes(String keycloakId) {
        return reprogrammerRepository.findByDemandeurKeycloakIdOrderByDateCreationDesc(keycloakId)
                .stream()
                .map(d -> toDto(d, interviewRepository.findById(d.getInterviewId()).orElse(null)))
                .collect(Collectors.toList());
    }

    public List<ReprogrammerDto> getDemandesATraiter(String keycloakId, boolean isRH) {
        List<Reprogrammer> demandes = isRH
                ? reprogrammerRepository.findByStatutOrderByDateCreationDesc(DemandeReportStatus.EN_ATTENTE)
                : reprogrammerRepository.findByCibleKeycloakIdAndStatutOrderByDateCreationDesc(
                        keycloakId, DemandeReportStatus.EN_ATTENTE);

        return demandes.stream()
                .map(d -> toDto(d, interviewRepository.findById(d.getInterviewId()).orElse(null)))
                .collect(Collectors.toList());
    }

    // ==================== MAPPING / HELPERS ====================

    private Interview getInterviewOuException(String interviewId) {
        return interviewRepository.findById(interviewId)
                .orElseThrow(() -> new TransitionStatutInvalideException("Entretien introuvable : " + interviewId));
    }

    private Reprogrammer getDemandeOuException(String demandeId) {
        return reprogrammerRepository.findById(demandeId)
                .orElseThrow(() -> new DemandeReportNotFoundException(demandeId));
    }

    private ReprogrammerDto toDto(Reprogrammer d, Interview interview) {
        return ReprogrammerDto.builder()
                .id(d.getIdReprogrammer())
                .interviewId(d.getInterviewId())
                .applicationId(d.getApplicationId())
                .type(d.getType())
                .demandeurKeycloakId(d.getDemandeurKeycloakId())
                .cibleKeycloakId(d.getCibleKeycloakId())
                .ancienneDate(d.getAncienneDate() != null ? d.getAncienneDate().toString() : null)
                .nouvelleDateProposee(d.getNouvelleDateProposee() != null ? d.getNouvelleDateProposee().toString() : null)
                .motif(d.getMotif())
                .statut(d.getStatut())
                .commentaireTraitement(d.getCommentaireTraitement())
                .traiteParKeycloakId(d.getTraiteParKeycloakId())
                .dateCreation(d.getDateCreation() != null ? d.getDateCreation().toString() : null)
                .dateTraitement(d.getDateTraitement() != null ? d.getDateTraitement().toString() : null)
                .candidateName(interview != null ? interview.getCandidateName() : null)
                .posteRecrutement(interview != null ? interview.getPosteRecrutement() : null)
                .interviewerName(interview != null ? interview.getInterviewerName() : null)
                .build();
    }
}