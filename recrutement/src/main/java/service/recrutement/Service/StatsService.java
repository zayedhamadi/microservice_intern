package service.recrutement.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.Enum.DemandeReportStatus;
import service.recrutement.Entity.Enum.InterviewResult;
import service.recrutement.Entity.Enum.InterviewStatus;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.Enum.StatusPosteRecrutement;
import service.recrutement.Entity.Enum.TypeDemandeReport;
import service.recrutement.Entity.dto.ApplicationStatsDto;
import service.recrutement.Entity.dto.DashboardStatsDto;
import service.recrutement.Entity.dto.EvolutionDto;
import service.recrutement.Entity.dto.InterviewsStatsDto;
import service.recrutement.Entity.dto.MonthDataDto;
import service.recrutement.Entity.dto.PostesStatsDto;
import service.recrutement.Entity.dto.ReprogrammerStatsDto;
import service.recrutement.Entity.dto.Projections.LabelCount;
import service.recrutement.Entity.dto.Projections.MonthCount;
import service.recrutement.Repository.ApplicationRepository;
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
public class StatsService {

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final PosteRecrutementRepository posteRecrutementRepository;
    private final ReprogrammerRepository reprogrammerRepository;

    /*
     * Ces statuts représentent tous les entretiens qui peuvent encore avoir
     * lieu dans le futur.
     *
     * EN_COURS n'est pas à venir.
     * TERMINE, ANNULE et ABSENT ne sont plus actifs.
     */
    private static final List<InterviewStatus> STATUTS_ENTRETIENS_A_VENIR =
            List.of(
                    InterviewStatus.PLANIFIE,
                    InterviewStatus.CONFIRME,
                    InterviewStatus.REPORTE
            );

    // ============================================================
    // DASHBOARD
    // ============================================================

    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto dashboard = DashboardStatsDto.builder()
                .applications(getApplicationStats())
                .interviews(getInterviewsStats())
                .postes(getPostesStats())
                .reprogrammations(getReprogrammerStats())
                .genereLe(LocalDateTime.now())
                .build();

        log.debug("Statistiques du dashboard RH générées");

        return dashboard;
    }

    // ============================================================
    // APPLICATIONS
    // ============================================================

    public ApplicationStatsDto getApplicationStats() {
        long total = applicationRepository.count();

        /*
         * toCompleteMap garantit la présence de toutes les possibilités :
         *
         * EN_ATTENTE
         * SELECTIONNE
         * EN_ENTRETIEN_RH
         * EN_ENTRETIEN_TECHNIQUE
         * EN_ENTRETIEN_FINAL
         * ACCEPTE
         * REJETE
         * RETIRE
         */
        Map<String, Long> parStatut = toCompleteMap(
                applicationRepository.countGroupByStatut(),
                nomsEnum(ApplicationStatus.class)
        );

        LocalDate debutMoisCourant = debutMoisCourant();
        LocalDate debutMoisSuivant = debutMoisCourant.plusMonths(1);
        LocalDate debutMoisPrecedent = debutMoisCourant.minusMonths(1);

        /*
         * Intervalles :
         * mois courant  : [début mois courant, début mois suivant[
         * mois précédent: [début mois précédent, début mois courant[
         */
        long ceMois = applicationRepository.countByDateCandidatureRange(
                debutMoisCourant,
                debutMoisSuivant
        );

        long moisDernier =
                applicationRepository.countByDateCandidatureRange(
                        debutMoisPrecedent,
                        debutMoisCourant
                );

        long acceptees = parStatut.getOrDefault(
                ApplicationStatus.ACCEPTE.name(),
                0L
        );

        long rejetees = parStatut.getOrDefault(
                ApplicationStatus.REJETE.name(),
                0L
        );

        double tauxAcceptation = calculerPourcentage(acceptees, total);
        double tauxRejet = calculerPourcentage(rejetees, total);

        return ApplicationStatsDto.builder()
                .total(total)
                .parStatut(parStatut)
                .evolution(evolution(total, ceMois, moisDernier))
                .tauxAcceptation(tauxAcceptation)
                .tauxRejet(tauxRejet)
                .serieMensuelle(
                        toFullMonthList(
                                applicationRepository.getMonthlyApplications()
                        )
                )
                .build();
    }

    // ============================================================
    // INTERVIEWS
    // ============================================================

    public InterviewsStatsDto getInterviewsStats() {
        long total = interviewRepository.count();

        /*
         * Cette map contient systématiquement toutes les possibilités :
         *
         * PLANIFIE
         * CONFIRME
         * EN_COURS
         * TERMINE
         * ANNULE
         * REPORTE
         * ABSENT
         *
         * Une valeur absente de MongoDB sera présente avec 0.
         */
        Map<String, Long> parStatut = toCompleteMap(
                interviewRepository.countGroupByStatut(),
                nomsEnum(InterviewStatus.class)
        );

        /*
         * Types d'entretien :
         *
         * RH_INITIAL
         * TECHNIQUE
         * RH_FINAL
         * LIBRE
         *
         * LIBRE représente source = LIBRE.
         */
        List<String> typesPossibles = new ArrayList<>(
                nomsEnum(InterviewType.class)
        );

        typesPossibles.add("LIBRE");

        Map<String, Long> parType = toCompleteMap(
                interviewRepository.countGroupByType(),
                typesPossibles
        );

        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime dansSeptJours = maintenant.plusDays(7);

        /*
         * Entretiens futurs, quelle que soit leur date :
         * PLANIFIE, CONFIRME ou REPORTE.
         */
        long aVenir =
                interviewRepository.countByStatutInAndDateEntretienAfter(
                        STATUTS_ENTRETIENS_A_VENIR,
                        maintenant
                );

        /*
         * Entretiens entre maintenant et maintenant + 7 jours :
         * PLANIFIE, CONFIRME ou REPORTE.
         */
        long dansLes7Jours =
                interviewRepository.countByStatutInAndDateEntretienBetween(
                        STATUTS_ENTRETIENS_A_VENIR,
                        maintenant,
                        dansSeptJours
                );

        long termines = parStatut.getOrDefault(
                InterviewStatus.TERMINE.name(),
                0L
        );

        long reussis = interviewRepository.countByStatutAndResultat(
                InterviewStatus.TERMINE,
                InterviewResult.REUSSI
        );

        double tauxReussite = calculerPourcentage(reussis, termines);

        return InterviewsStatsDto.builder()
                .total(total)
                .parStatut(parStatut)
                .parType(parType)
                .aVenir(aVenir)
                .dansLes7Jours(dansLes7Jours)
                .tauxReussite(tauxReussite)
                .serieMensuelle(
                        toFullMonthList(
                                interviewRepository.getMonthlyInterviews()
                        )
                )
                .build();
    }

    // ============================================================
    // POSTES
    // ============================================================

    public PostesStatsDto getPostesStats() {
        long total = posteRecrutementRepository.count();

        /*
         * Toutes les possibilités sont présentes :
         *
         * OUVERT
         * EXPIRE
         * FERME
         */
        Map<String, Long> parStatut = toCompleteMap(
                posteRecrutementRepository.countGroupByStatus(),
                nomsEnum(StatusPosteRecrutement.class)
        );

        Map<String, Long> parDepartement = toMap(
                posteRecrutementRepository.countGroupByDepartement()
        );

        LocalDate aujourdHui = LocalDate.now();

        /*
         * Un poste est réellement ouvert si :
         * - status = OUVERT
         * - et sa date d'expiration est absente ou >= aujourd'hui.
         */
        long ouverts =
                posteRecrutementRepository.countOuvertsEffectifs(
                        StatusPosteRecrutement.OUVERT,
                        aujourdHui
                );

        long expirantSous7Jours =
                posteRecrutementRepository
                        .countByStatusAndDateExpirationPosteRecrutementBetween(
                                StatusPosteRecrutement.OUVERT,
                                aujourdHui,
                                aujourdHui.plusDays(7)
                        );

        LocalDate debutMoisCourant = debutMoisCourant();
        LocalDate debutMoisSuivant = debutMoisCourant.plusMonths(1);
        LocalDate debutMoisPrecedent = debutMoisCourant.minusMonths(1);

        long ceMois =
                posteRecrutementRepository
                        .countByDatePosteRecrutementRange(
                                debutMoisCourant,
                                debutMoisSuivant
                        );

        long moisDernier =
                posteRecrutementRepository
                        .countByDatePosteRecrutementRange(
                                debutMoisPrecedent,
                                debutMoisCourant
                        );

        return PostesStatsDto.builder()
                .total(total)
                .ouverts(ouverts)
                .parStatut(parStatut)
                .parDepartement(parDepartement)
                .expirantSous7Jours(expirantSous7Jours)
                .evolution(evolution(total, ceMois, moisDernier))
                .serieMensuelle(
                        toFullMonthList(
                                posteRecrutementRepository.getMonthlyPostes()
                        )
                )
                .build();
    }

    // ============================================================
    // REPROGRAMMATIONS
    // ============================================================

    public ReprogrammerStatsDto getReprogrammerStats() {
        long total = reprogrammerRepository.count();

        /*
         * Toutes les possibilités sont présentes :
         *
         * EN_ATTENTE
         * ACCEPTEE
         * REFUSEE
         */
        Map<String, Long> parStatut = toCompleteMap(
                reprogrammerRepository.countGroupByStatut(),
                nomsEnum(DemandeReportStatus.class)
        );

        /*
         * Toutes les possibilités sont présentes :
         *
         * DEMANDE_CANDIDAT
         * PROPOSITION_INTERVENANT
         * REACTIVATION_APRES_ABSENCE
         */
        Map<String, Long> parType = toCompleteMap(
                reprogrammerRepository.countGroupByType(),
                nomsEnum(TypeDemandeReport.class)
        );

        long enAttente = parStatut.getOrDefault(
                DemandeReportStatus.EN_ATTENTE.name(),
                0L
        );

        return ReprogrammerStatsDto.builder()
                .total(total)
                .enAttente(enAttente)
                .parStatut(parStatut)
                .parType(parType)
                .serieMensuelle(
                        toFullMonthList(
                                reprogrammerRepository
                                        .getMonthlyReprogrammations()
                        )
                )
                .build();
    }

    // ============================================================
    // HELPERS : MAPS
    // ============================================================

    private Map<String, Long> toMap(List<LabelCount> liste) {
        Map<String, Long> resultat = new LinkedHashMap<>();

        if (liste == null) {
            return resultat;
        }

        for (LabelCount item : liste) {
            if (item == null) {
                continue;
            }

            String cle = normaliserCle(item.getId());

            /*
             * Long::sum évite de perdre des données s'il existe plusieurs
             * groupes qui donnent la même clé après normalisation.
             */
            resultat.merge(cle, item.getTotal(), Long::sum);
        }

        return resultat;
    }

    private Map<String, Long> toCompleteMap(
            List<LabelCount> liste,
            List<String> toutesLesCles
    ) {
        Map<String, Long> valeursMongo = toMap(liste);
        Map<String, Long> resultat = new LinkedHashMap<>();

        /*
         * Ajouter toutes les valeurs attendues dans l'ordre de l'enum.
         */
        for (String cle : toutesLesCles) {
            resultat.put(
                    cle,
                    valeursMongo.getOrDefault(cle, 0L)
            );
        }

        /*
         * Conserver les anciennes valeurs ou les valeurs imprévues de MongoDB,
         * par exemple INCONNU.
         */
        valeursMongo.forEach(resultat::putIfAbsent);

        return resultat;
    }

    private String normaliserCle(String cle) {
        if (cle == null || cle.isBlank()) {
            return "INCONNU";
        }

        return cle.trim();
    }

    private List<String> nomsEnum(
            Class<? extends Enum<?>> enumClass
    ) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    // ============================================================
    // HELPERS : MOIS
    // ============================================================

    private List<MonthDataDto> toFullMonthList(
            List<MonthCount> liste
    ) {
        Map<Integer, Long> valeursParMois = new LinkedHashMap<>();

        if (liste != null) {
            for (MonthCount item : liste) {
                if (item == null || item.getMois() == null) {
                    continue;
                }

                int mois = item.getMois();

                if (mois < 1 || mois > 12) {
                    log.warn(
                            "Numéro de mois invalide ignoré dans les statistiques : {}",
                            mois
                    );
                    continue;
                }

                valeursParMois.merge(
                        mois,
                        item.getTotal(),
                        Long::sum
                );
            }
        }

        List<MonthDataDto> resultat = new ArrayList<>(12);

        for (int mois = 1; mois <= 12; mois++) {
            resultat.add(
                    MonthDataDto.builder()
                            .mois(mois)
                            .total(
                                    valeursParMois.getOrDefault(
                                            mois,
                                            0L
                                    )
                            )
                            .build()
            );
        }

        return resultat;
    }

    private LocalDate debutMoisCourant() {
        return LocalDate.now().withDayOfMonth(1);
    }

    // ============================================================
    // HELPERS : POURCENTAGES ET ÉVOLUTION
    // ============================================================

    private double calculerPourcentage(
            long valeur,
            long total
    ) {
        if (total <= 0) {
            return 0.0;
        }

        return (valeur * 100.0) / total;
    }

    private EvolutionDto evolution(
            long total,
            long ceMois,
            long moisDernier
    ) {
        double variation;

        if (moisDernier == 0) {
            variation = ceMois == 0
                    ? 0.0
                    : 100.0;
        } else {
            variation =
                    ((ceMois - moisDernier) * 100.0)
                            / moisDernier;
        }

        return EvolutionDto.builder()
                .total(total)
                .ceMois(ceMois)
                .moisDernier(moisDernier)
                .variationPourcent(variation)
                .build();
    }
}