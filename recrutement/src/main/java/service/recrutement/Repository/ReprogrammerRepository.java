package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Enum.DemandeReportStatus;
import service.recrutement.Entity.Reprogrammer;
import service.recrutement.Entity.dto.Projections.LabelCount;
import service.recrutement.Entity.dto.Projections.MonthCount;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReprogrammerRepository extends MongoRepository<Reprogrammer, String> {
    long countByDemandeurKeycloakId(String demandeurKeycloakId);

    long countByDemandeurKeycloakIdAndStatut(
            String demandeurKeycloakId,
            DemandeReportStatus statut
    );

    @Aggregation(pipeline = {
            "{ $match: { demandeurKeycloakId: ?0 } }",
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
    List<LabelCount> countGroupByStatutForDemandeur(
            String demandeurKeycloakId
    );

    List<Reprogrammer> findByInterviewIdOrderByDateCreationDesc(String interviewId);

    List<Reprogrammer> findByDemandeurKeycloakIdOrderByDateCreationDesc(String demandeurKeycloakId);

    List<Reprogrammer> findByCibleKeycloakIdAndStatutOrderByDateCreationDesc(
            String cibleKeycloakId, DemandeReportStatus statut);

    List<Reprogrammer> findByStatutOrderByDateCreationDesc(DemandeReportStatus statut);

    boolean existsByInterviewIdAndStatut(String interviewId, DemandeReportStatus statut);

    // ==================== STATS ====================

    long countByStatut(DemandeReportStatus statut);

    long countByDateCreationBetween(LocalDateTime debut, LocalDateTime fin);

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
            "{ $group: { " +
                    "_id: { $ifNull: ['$type', 'INCONNU'] }, " +
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
                    "dateCreation: { $ne: null }, " +
                    "$expr: { " +
                    "$eq: [ " +
                    "{ $year: '$dateCreation' }, " +
                    "{ $year: '$$NOW' } " +
                    "] " +
                    "} " +
                    "} }",
            "{ $group: { " +
                    "_id: { $month: '$dateCreation' }, " +
                    "total: { $sum: 1 } " +
                    "} }",
            "{ $project: { " +
                    "_id: 0, " +
                    "mois: '$_id', " +
                    "total: 1 " +
                    "} }",
            "{ $sort: { mois: 1 } }"
    })
    List<MonthCount> getMonthlyReprogrammations();
}