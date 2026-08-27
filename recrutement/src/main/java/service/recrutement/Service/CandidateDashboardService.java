package service.recrutement.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.Enum.DemandeReportStatus;
import service.recrutement.Entity.Enum.InterviewResult;
import service.recrutement.Entity.Enum.InterviewStatus;
import service.recrutement.Entity.Enum.StatusPosteRecrutement;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Interview;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.ApplicationCandidateStatsDto;
import service.recrutement.Entity.dto.CandidateDashboardStatsDto;
import service.recrutement.Entity.dto.CertificationCandidateStatsDto;
import service.recrutement.Entity.dto.InterviewCandidateStatsDto;
import service.recrutement.Entity.dto.MonthDataDto;
import service.recrutement.Entity.dto.PosteCandidateItemDto;
import service.recrutement.Entity.dto.PosteCandidateStatsDto;
import service.recrutement.Entity.dto.ReprogrammerCandidateStatsDto;
import service.recrutement.Entity.dto.UpcomingInterviewDto;
import service.recrutement.Entity.dto.Projections.AverageScore;
import service.recrutement.Entity.dto.Projections.LabelCount;
import service.recrutement.Entity.dto.Projections.MonthCount;
import service.recrutement.Repository.ApplicationRepository;
import service.recrutement.Repository.CertificationRepository;
import service.recrutement.Repository.InterviewRepository;
import service.recrutement.Repository.PosteRecrutementRepository;
import service.recrutement.Repository.ReprogrammerRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CandidateDashboardService {

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final PosteRecrutementRepository posteRecrutementRepository;
    private final CertificationRepository certificationRepository;
    private final ReprogrammerRepository reprogrammerRepository;

    private static final List<InterviewStatus> STATUTS_A_VENIR =
            List.of(
                    InterviewStatus.PLANIFIE,
                    InterviewStatus.CONFIRME,
                    InterviewStatus.REPORTE
            );

    public CandidateDashboardStatsDto getDashboard(
            String candidatKeycloakId
    ) {
        if (candidatKeycloakId == null ||
                candidatKeycloakId.isBlank()) {
            throw new IllegalArgumentException(
                    "L'identifiant Keycloak du candidat est obligatoire"
            );
        }

        return CandidateDashboardStatsDto.builder()
                .candidatKeycloakId(candidatKeycloakId)
                .applications(
                        getApplicationStats(candidatKeycloakId)
                )
                .interviews(
                        getInterviewStats(candidatKeycloakId)
                )
                .postes(
                        getPosteStats()
                )
                .certifications(
                        getCertificationStats(candidatKeycloakId)
                )
                .reprogrammations(
                        getReprogrammerStats(candidatKeycloakId)
                )
                .genereLe(LocalDateTime.now())
                .build();
    }

    private ApplicationCandidateStatsDto getApplicationStats(
            String candidatKeycloakId
    ) {
        long total =
                applicationRepository.countByCandidatKeycloakId(
                        candidatKeycloakId
                );

        Map<String, Long> parStatut =
                toCompleteMap(
                        applicationRepository
                                .countGroupByStatutForCandidate(
                                        candidatKeycloakId
                                ),
                        nomsEnum(ApplicationStatus.class)
                );

        LocalDate debutMois =
                LocalDate.now().withDayOfMonth(1);

        LocalDate debutMoisSuivant =
                debutMois.plusMonths(1);

        long ceMois =
                applicationRepository
                        .countByCandidatKeycloakIdAndDateCandidatureRange(
                                candidatKeycloakId,
                                debutMois,
                                debutMoisSuivant
                        );

        long acceptees =
                parStatut.getOrDefault(
                        ApplicationStatus.ACCEPTE.name(),
                        0L
                );

        long rejetees =
                parStatut.getOrDefault(
                        ApplicationStatus.REJETE.name(),
                        0L
                );

        long retirees =
                parStatut.getOrDefault(
                        ApplicationStatus.RETIRE.name(),
                        0L
                );

        long enCours =
                parStatut.getOrDefault(
                        ApplicationStatus.EN_ATTENTE.name(),
                        0L
                )
                + parStatut.getOrDefault(
                        ApplicationStatus.SELECTIONNE.name(),
                        0L
                )
                + parStatut.getOrDefault(
                        ApplicationStatus.EN_ENTRETIEN_RH.name(),
                        0L
                )
                + parStatut.getOrDefault(
                        ApplicationStatus.EN_ENTRETIEN_TECHNIQUE.name(),
                        0L
                )
                + parStatut.getOrDefault(
                        ApplicationStatus.EN_ENTRETIEN_FINAL.name(),
                        0L
                );

        // BUG FIX : le taux d'acceptation était calculé sur le total (y compris
        // les candidatures encore en cours), ce qui le diluait artificiellement.
        // Il est maintenant calculé parmi les candidatures effectivement traitées
        // (acceptees + rejetees), cohérent avec le calcul de tauxReussite côté entretiens.
        double tauxAcceptation =
                calculerPourcentage(acceptees, acceptees + rejetees);

        AverageScore scoreMoyenProjection =
                applicationRepository
                        .getAverageScoreMatchingForCandidate(
                                candidatKeycloakId
                        );

        Double scoreMatchingMoyen =
                scoreMoyenProjection != null
                        ? scoreMoyenProjection.getMoyenne()
                        : null;

        return ApplicationCandidateStatsDto.builder()
                .total(total)
                .parStatut(parStatut)
                .ceMois(ceMois)
                .acceptees(acceptees)
                .rejetees(rejetees)
                .enCours(enCours)
                .retirees(retirees)
                .tauxAcceptation(tauxAcceptation)
                .scoreMatchingMoyen(scoreMatchingMoyen)
                .serieMensuelle(
                        toFullMonthList(
                                applicationRepository
                                        .getMonthlyApplicationsForCandidate(
                                                candidatKeycloakId
                                        )
                        )
                )
                .build();
    }

    private InterviewCandidateStatsDto getInterviewStats(
            String candidatKeycloakId
    ) {
        long total =
                interviewRepository.countByCandidatKeycloakId(
                        candidatKeycloakId
                );

        Map<String, Long> parStatut =
                toCompleteMap(
                        interviewRepository
                                .countGroupByStatutForCandidate(
                                        candidatKeycloakId
                                ),
                        nomsEnum(InterviewStatus.class)
                );

        LocalDateTime maintenant =
                LocalDateTime.now();

        LocalDateTime dansSeptJours =
                maintenant.plusDays(7);

        long aVenir =
                interviewRepository
                        .countByCandidatKeycloakIdAndStatutInAndDateEntretienAfter(
                                candidatKeycloakId,
                                STATUTS_A_VENIR,
                                maintenant
                        );

        long dansLes7Jours =
                interviewRepository
                        .countByCandidatKeycloakIdAndStatutInAndDateEntretienBetween(
                                candidatKeycloakId,
                                STATUTS_A_VENIR,
                                maintenant,
                                dansSeptJours
                        );

        long termines =
                parStatut.getOrDefault(
                        InterviewStatus.TERMINE.name(),
                        0L
                );

        long reussis =
                interviewRepository
                        .countByCandidatKeycloakIdAndStatutAndResultat(
                                candidatKeycloakId,
                                InterviewStatus.TERMINE,
                                InterviewResult.REUSSI
                        );

        List<Interview> interviews =
                interviewRepository
                        .findTop5ByCandidatKeycloakIdAndStatutInAndDateEntretienAfterOrderByDateEntretienAsc(
                                candidatKeycloakId,
                                STATUTS_A_VENIR,
                                maintenant
                        );

        List<UpcomingInterviewDto> prochains =
                interviews.stream()
                        .map(this::toUpcomingInterviewDto)
                        .toList();

        return InterviewCandidateStatsDto.builder()
                .total(total)
                .aVenir(aVenir)
                .dansLes7Jours(dansLes7Jours)
                .termines(termines)
                .reussis(reussis)
                .tauxReussite(
                        calculerPourcentage(reussis, termines)
                )
                .parStatut(parStatut)
                .prochains(prochains)
                .build();
    }

    private PosteCandidateStatsDto getPosteStats() {
        LocalDate aujourdHui = LocalDate.now();

        long totalOuverts =
                posteRecrutementRepository
                        .countEffectiveOpenPosts(
                                StatusPosteRecrutement.OUVERT,
                                aujourdHui
                        );

        List<PosteRecrutement> postes =
                posteRecrutementRepository
                        .findLatestOpenPosts(aujourdHui);

        List<PosteCandidateItemDto> derniersPostes =
                postes.stream()
                        .map(this::toPosteDto)
                        .toList();

        return PosteCandidateStatsDto.builder()
                .totalOuverts(totalOuverts)
                .derniersPostes(derniersPostes)
                .build();
    }

    private CertificationCandidateStatsDto getCertificationStats(
            String candidatKeycloakId
    ) {
        long total =
                certificationRepository
                        .countByKeycloakId(candidatKeycloakId);

        return CertificationCandidateStatsDto.builder()
                .total(total)
                .build();
    }

    /**
     * Nouvelle méthode : stats des demandes de report d'entretien faites
     * par le candidat lui-même (Reprogrammer.demandeurKeycloakId).
     * Absente auparavant du dashboard candidat alors qu'elle existe déjà
     * côté dashboard RH.
     */
    private ReprogrammerCandidateStatsDto getReprogrammerStats(
            String candidatKeycloakId
    ) {
        long total =
                reprogrammerRepository
                        .countByDemandeurKeycloakId(candidatKeycloakId);

        Map<String, Long> parStatut =
                toCompleteMap(
                        reprogrammerRepository
                                .countGroupByStatutForDemandeur(
                                        candidatKeycloakId
                                ),
                        nomsEnum(DemandeReportStatus.class)
                );

        long enAttente =
                parStatut.getOrDefault(
                        DemandeReportStatus.EN_ATTENTE.name(),
                        0L
                );

        long acceptees =
                parStatut.getOrDefault(
                        DemandeReportStatus.ACCEPTEE.name(),
                        0L
                );

        long refusees =
                parStatut.getOrDefault(
                        DemandeReportStatus.REFUSEE.name(),
                        0L
                );

        return ReprogrammerCandidateStatsDto.builder()
                .total(total)
                .enAttente(enAttente)
                .acceptees(acceptees)
                .refusees(refusees)
                .parStatut(parStatut)
                .build();
    }

    private UpcomingInterviewDto toUpcomingInterviewDto(
            Interview interview
    ) {
        return UpcomingInterviewDto.builder()
                .idInterview(interview.getIdInterview())
                .applicationId(interview.getApplicationId())
                .posteRecrutement(interview.getPosteRecrutement())
                .posteId(interview.getPosteId())
                .type(interview.getType())
                .source(interview.getSource())
                .mode(interview.getMode())
                .dateEntretien(interview.getDateEntretien())
                .dateFinEntretien(interview.getDateFinEntretien())
                .statut(interview.getStatut())
                .lieu(interview.getLieu())
                .lienVisio(interview.getLienVisio())
                .interviewerName(interview.getInterviewerName())
                .build();
    }

    private PosteCandidateItemDto toPosteDto(
            PosteRecrutement poste
    ) {
        return PosteCandidateItemDto.builder()
                .idPosteRecrutement(
                        poste.getIdPosteRecrutement()
                )
                .titre(poste.getTitre())
                .departementNom(poste.getDepartementNom())
                .typeContrat(poste.getTypeContrat())
                .workType(poste.getWorkType())
                .lieu(poste.getLieu())
                .salaire(poste.getSalaire())
                .nombrePostes(poste.getNombrePostes())
                .datePosteRecrutement(
                        poste.getDatePosteRecrutement()
                )
                .dateExpirationPosteRecrutement(
                        poste.getDateExpirationPosteRecrutement()
                )
                .build();
    }

    private Map<String, Long> toMap(
            List<LabelCount> liste
    ) {
        Map<String, Long> result =
                new LinkedHashMap<>();

        if (liste == null) {
            return result;
        }

        for (LabelCount item : liste) {
            if (item == null) {
                continue;
            }

            String key = item.getId();

            if (key == null || key.isBlank()) {
                key = "INCONNU";
            }

            result.merge(
                    key.trim(),
                    item.getTotal(),
                    Long::sum
            );
        }

        return result;
    }

    private Map<String, Long> toCompleteMap(
            List<LabelCount> liste,
            List<String> toutesLesCles
    ) {
        Map<String, Long> brut = toMap(liste);
        Map<String, Long> result = new LinkedHashMap<>();

        for (String cle : toutesLesCles) {
            result.put(
                    cle,
                    brut.getOrDefault(cle, 0L)
            );
        }

        brut.forEach(result::putIfAbsent);

        return result;
    }

    private List<String> nomsEnum(
            Class<? extends Enum<?>> enumClass
    ) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    private List<MonthDataDto> toFullMonthList(
            List<MonthCount> liste
    ) {
        Map<Integer, Long> parMois =
                new LinkedHashMap<>();

        if (liste != null) {
            for (MonthCount item : liste) {
                if (item == null ||
                        item.getMois() == null) {
                    continue;
                }

                parMois.merge(
                        item.getMois(),
                        item.getTotal(),
                        Long::sum
                );
            }
        }

        List<MonthDataDto> result =
                new ArrayList<>();

        for (int mois = 1; mois <= 12; mois++) {
            result.add(
                    MonthDataDto.builder()
                            .mois(mois)
                            .total(
                                    parMois.getOrDefault(
                                            mois,
                                            0L
                                    )
                            )
                            .build()
            );
        }

        return result;
    }

    private double calculerPourcentage(
            long valeur,
            long total
    ) {
        if (total <= 0) {
            return 0.0;
        }

        return valeur * 100.0 / total;
    }
}