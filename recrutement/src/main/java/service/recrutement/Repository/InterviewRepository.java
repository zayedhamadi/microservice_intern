package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Enum.InterviewResult;
import service.recrutement.Entity.Enum.InterviewSource;
import service.recrutement.Entity.Enum.InterviewStatus;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.Interview;
import service.recrutement.Entity.dto.Projections.LabelCount;
import service.recrutement.Entity.dto.Projections.MonthCount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository
        extends MongoRepository<Interview, String> {


    List<Interview> findByCandidatKeycloakId(
            String candidatKeycloakId
    );

    List<Interview> findByCandidatKeycloakIdAndType(
            String candidatKeycloakId,
            InterviewType type
    );

    Optional<Interview>
    findFirstByCandidatKeycloakIdAndDateEntretienAfterOrderByDateEntretienAsc(
            String candidatKeycloakId,
            LocalDateTime after
    );

    List<Interview>
    findByRecruteurKeycloakIdOrderByDateEntretienAsc(
            String recruteurKeycloakId
    );

    List<Interview>
    findByRecruteurKeycloakIdAndTypeAndSource(
            String recruteurKeycloakId,
            InterviewType type,
            InterviewSource source
    );

    List<Interview> findBySource(
            InterviewSource source
    );

    List<Interview>
    findByApplicationIdOrderByDateCreationDesc(
            String applicationId
    );

    List<Interview> findByTypeAndSource(
            InterviewType type,
            InterviewSource source
    );

    boolean existsByApplicationIdAndTypeAndStatutIn(
            String applicationId,
            InterviewType type,
            List<InterviewStatus> statuts
    );



    long countByCandidatKeycloakId(
            String candidatKeycloakId
    );

    long countByCandidatKeycloakIdAndStatut(
            String candidatKeycloakId,
            InterviewStatus statut
    );

    long countByCandidatKeycloakIdAndStatutInAndDateEntretienAfter(
            String candidatKeycloakId,
            List<InterviewStatus> statuts,
            LocalDateTime date
    );

    long countByCandidatKeycloakIdAndStatutInAndDateEntretienBetween(
            String candidatKeycloakId,
            List<InterviewStatus> statuts,
            LocalDateTime debut,
            LocalDateTime fin
    );

    long countByCandidatKeycloakIdAndStatutAndResultat(
            String candidatKeycloakId,
            InterviewStatus statut,
            InterviewResult resultat
    );

    List<Interview>
    findTop5ByCandidatKeycloakIdAndStatutInAndDateEntretienAfterOrderByDateEntretienAsc(
            String candidatKeycloakId,
            List<InterviewStatus> statuts,
            LocalDateTime date
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




    long countByStatut(
            InterviewStatus statut
    );

    long countByType(
            InterviewType type
    );

    long countByStatutAndResultat(
            InterviewStatus statut,
            InterviewResult resultat
    );

    long countByStatutInAndDateEntretienAfter(
            List<InterviewStatus> statuts,
            LocalDateTime apres
    );

    long countByStatutInAndDateEntretienBetween(
            List<InterviewStatus> statuts,
            LocalDateTime debut,
            LocalDateTime fin
    );

    long countByDateCreationBetween(
            LocalDateTime debut,
            LocalDateTime fin
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
            "{ $project: { " +
                    "typeDashboard: { " +
                    "$cond: [ " +
                    "{ $eq: ['$source', 'LIBRE'] }, " +
                    "'LIBRE', " +
                    "{ $ifNull: ['$type', 'INCONNU'] } " +
                    "] " +
                    "} " +
                    "} }",

            "{ $group: { " +
                    "_id: '$typeDashboard', " +
                    "total: { $sum: 1 } " +
                    "} }",

            "{ $project: { " +
                    "_id: 0, " +
                    "id: '$_id', " +
                    "total: 1 " +
                    "} }",

            "{ $sort: { id: 1 } }"
    })
    List<LabelCount> countGroupByType();

    @Aggregation(pipeline = {
            "{ $match: { " +
                    "dateEntretien: { $ne: null }, " +
                    "$expr: { " +
                    "$eq: [ " +
                    "{ $year: '$dateEntretien' }, " +
                    "{ $year: '$$NOW' } " +
                    "] " +
                    "} " +
                    "} }",

            "{ $group: { " +
                    "_id: { $month: '$dateEntretien' }, " +
                    "total: { $sum: 1 } " +
                    "} }",

            "{ $project: { " +
                    "_id: 0, " +
                    "mois: '$_id', " +
                    "total: 1 " +
                    "} }",

            "{ $sort: { mois: 1 } }"
    })
    List<MonthCount> getMonthlyInterviews();
}