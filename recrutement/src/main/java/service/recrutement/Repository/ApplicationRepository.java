package service.recrutement.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.dto.ApplicationDto;
import service.recrutement.Entity.dto.Projections.AverageScore;
import service.recrutement.Entity.dto.Projections.LabelCount;
import service.recrutement.Entity.dto.Projections.MonthCount;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository
        extends MongoRepository<Application, String> {
    @Aggregation(pipeline = {
            "{ $match: { " +
                    "candidatKeycloakId: ?0, " +
                    "scoreMatching: { $ne: null } " +
                    "} }",

            "{ $group: { " +
                    "_id: null, " +
                    "moyenne: { $avg: '$scoreMatching' } " +
                    "} }"
    })
    AverageScore getAverageScoreMatchingForCandidate(
            String candidatKeycloakId
    );

    Page<Application> findByCandidatKeycloakId(
            String candidatKeycloakId,
            Pageable pageable
    );

    Page<Application> findByCandidatKeycloakIdAndStatut(
            String candidatKeycloakId,
            ApplicationStatus statut,
            Pageable pageable
    );

    List<Application> findByCandidatKeycloakId(
            String candidatKeycloakId
    );


    long countByCandidatKeycloakId(
            String candidatKeycloakId
    );

    Optional<Application> findByCandidatKeycloakIdAndPosteRecrutementId(
            String candidatKeycloakId,
            String posteRecrutementId
    );

    List<Application> findByStatut(
            ApplicationStatus statut
    );

    boolean existsByCandidatKeycloakIdAndPosteRecrutementId(
            String candidatKeycloakId,
            String posteRecrutementId
    );

    List<Application>
    findByCandidatKeycloakIdOrderByDateCandidatureDesc(
            String candidatKeycloakId
    );

    List<Application>
    findByPosteRecrutementIdOrderByDateCandidatureDesc(
            String posteRecrutementId
    );

    List<Application> findByPosteRecrutementIdAndStatut(
            String posteRecrutementId,
            ApplicationStatus statut
    );

    long countByPosteRecrutementId(
            String posteRecrutementId
    );

    Optional<ApplicationDto> findByIdApplication(
            String idApplication
    );

    List<Application> findByPosteRecrutementId(
            String posteRecrutementId
    );

    long countByStatut(
            ApplicationStatus statut
    );

    long countByCandidatKeycloakIdAndStatut(
            String candidatKeycloakId,
            ApplicationStatus statut
    );

    @Aggregation(pipeline = {
            "{ $match: { " +
                    "candidatKeycloakId: ?0 " +
                    "} }",

            "{ $group: { " +
                    "_id: { $ifNull: ['$statut', 'INCONNU'] }, " +
                    "total: { $sum: 1 } " +
                    "} }",

            "{ $project: { " +
                    "_id: 0, " +
                    "id: '$_id', " +
                    "total: 1 " +
                    "} }",

            "{ $sort: { id: 1 } }"
    })
    List<LabelCount> countGroupByStatutForCandidate(
            String candidatKeycloakId
    );

    @Query(
            value = "{ " +
                    "candidatKeycloakId: ?0, " +
                    "dateCandidature: { " +
                    "$gte: ?1, " +
                    "$lt: ?2 " +
                    "} " +
                    "}",
            count = true
    )
    long countByCandidatKeycloakIdAndDateCandidatureRange(
            String candidatKeycloakId,
            LocalDate debut,
            LocalDate finExclusive
    );


    @Aggregation(pipeline = {
            "{ $match: { " +
                    "candidatKeycloakId: ?0, " +
                    "dateCandidature: { $ne: null } " +
                    "} }",

            "{ $match: { " +
                    "$expr: { " +
                    "$eq: [ " +
                    "{ $year: '$dateCandidature' }, " +
                    "{ $year: '$$NOW' } " +
                    "] " +
                    "} " +
                    "} }",

            "{ $group: { " +
                    "_id: { $month: '$dateCandidature' }, " +
                    "total: { $sum: 1 } " +
                    "} }",

            "{ $project: { " +
                    "_id: 0, " +
                    "mois: '$_id', " +
                    "total: 1 " +
                    "} }",

            "{ $sort: { mois: 1 } }"
    })
    List<MonthCount> getMonthlyApplicationsForCandidate(
            String candidatKeycloakId
    );


    @Query(
            value = "{ " +
                    "dateCandidature: { " +
                    "$gte: ?0, " +
                    "$lt: ?1 " +
                    "} " +
                    "}",
            count = true
    )
    long countByDateCandidatureRange(
            LocalDate debut,
            LocalDate finExclusive
    );

    @Aggregation(pipeline = {
            "{ $group: { " +
                    "_id: { $ifNull: ['$statut', 'INCONNU'] }, " +
                    "total: { $sum: 1 } " +
                    "} }",

            "{ $project: { " +
                    "_id: 0, " +
                    "id: '$_id', " +
                    "total: 1 " +
                    "} }",

            "{ $sort: { id: 1 } }"
    })
    List<LabelCount> countGroupByStatut();

    @Aggregation(pipeline = {
            "{ $match: { " +
                    "dateCandidature: { $ne: null }, " +
                    "$expr: { " +
                    "$eq: [ " +
                    "{ $year: '$dateCandidature' }, " +
                    "{ $year: '$$NOW' } " +
                    "] " +
                    "} " +
                    "} }",

            "{ $group: { " +
                    "_id: { $month: '$dateCandidature' }, " +
                    "total: { $sum: 1 } " +
                    "} }",

            "{ $project: { " +
                    "_id: 0, " +
                    "mois: '$_id', " +
                    "total: 1 " +
                    "} }",

            "{ $sort: { mois: 1 } }"
    })
    List<MonthCount> getMonthlyApplications();
}