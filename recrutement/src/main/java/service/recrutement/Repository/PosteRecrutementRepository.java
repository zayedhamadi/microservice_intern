package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Enum.StatusPosteRecrutement;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.Projections.LabelCount;
import service.recrutement.Entity.dto.Projections.MonthCount;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PosteRecrutementRepository
        extends MongoRepository<PosteRecrutement, String> {

    Optional<PosteRecrutement> findByIdPosteRecrutement(
            String idPosteRecrutement
    );

    List<PosteRecrutement> findByDepartementNom(
            String departementNom
    );

    long countByStatus(
            StatusPosteRecrutement status
    );

    @Query(
            value = "{ " +
                    "status: ?0, " +
                    "$or: [ " +
                    "{ dateExpirationPosteRecrutement: null }, " +
                    "{ dateExpirationPosteRecrutement: { $gte: ?1 } } " +
                    "] " +
                    "}",
            count = true
    )
    long countOuvertsEffectifs(
            StatusPosteRecrutement status,
            LocalDate today
    );

    @Query(
            value = "{ " +
                    "status: ?0, " +
                    "$or: [ " +
                    "{ dateExpirationPosteRecrutement: null }, " +
                    "{ dateExpirationPosteRecrutement: { $gte: ?1 } } " +
                    "] " +
                    "}",
            count = true
    )
    long countEffectiveOpenPosts(
            StatusPosteRecrutement status,
            LocalDate aujourdHui
    );

    @Aggregation(pipeline = {
            "{ $match: { " +
                    "status: 'OUVERT', " +
                    "$or: [ " +
                    "{ dateExpirationPosteRecrutement: null }, " +
                    "{ dateExpirationPosteRecrutement: { $gte: ?0 } } " +
                    "] " +
                    "} }",

            "{ $sort: { dateCreation: -1 } }",

            "{ $limit: 5 }"
    })
    List<PosteRecrutement> findLatestOpenPosts(
            LocalDate aujourdHui
    );

    @Query(
            value = "{ " +
                    "datePosteRecrutement: { " +
                    "$gte: ?0, " +
                    "$lt: ?1 " +
                    "} " +
                    "}",
            count = true
    )
    long countByDatePosteRecrutementRange(
            LocalDate debut,
            LocalDate finExclusive
    );

    long countByStatusAndDateExpirationPosteRecrutementBetween(
            StatusPosteRecrutement status,
            LocalDate debut,
            LocalDate fin
    );

    @Aggregation(pipeline = {
            "{ $group: { " +
                    "_id: { $ifNull: ['$status', 'INCONNU'] }, " +
                    "total: { $sum: 1 } " +
                    "} }",

            "{ $project: { " +
                    "_id: 0, " +
                    "id: '$_id', " +
                    "total: 1 " +
                    "} }",

            "{ $sort: { id: 1 } }"
    })
    List<LabelCount> countGroupByStatus();

    @Aggregation(pipeline = {
            "{ $group: { " +
                    "_id: { " +
                    "$cond: [ " +
                    "{ $or: [ " +
                    "{ $eq: ['$departementNom', null] }, " +
                    "{ $eq: ['$departementNom', ''] } " +
                    "] }, " +
                    "'NON_DEFINI', " +
                    "'$departementNom' " +
                    "] " +
                    "}, " +
                    "total: { $sum: 1 } " +
                    "} }",

            "{ $project: { " +
                    "_id: 0, " +
                    "id: '$_id', " +
                    "total: 1 " +
                    "} }",

            "{ $sort: { id: 1 } }"
    })
    List<LabelCount> countGroupByDepartement();

    @Aggregation(pipeline = {
            "{ $match: { " +
                    "datePosteRecrutement: { $ne: null }, " +
                    "$expr: { " +
                    "$eq: [ " +
                    "{ $year: '$datePosteRecrutement' }, " +
                    "{ $year: '$$NOW' } " +
                    "] " +
                    "} " +
                    "} }",

            "{ $group: { " +
                    "_id: { $month: '$datePosteRecrutement' }, " +
                    "total: { $sum: 1 } " +
                    "} }",

            "{ $project: { " +
                    "_id: 0, " +
                    "mois: '$_id', " +
                    "total: 1 " +
                    "} }",

            "{ $sort: { mois: 1 } }"
    })
    List<MonthCount> getMonthlyPostes();
}