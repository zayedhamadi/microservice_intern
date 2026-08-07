package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.ApplicationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends MongoRepository<Application, String> {

    Optional<Application> findByCandidatKeycloakIdAndPosteRecrutementId(
            String candidatKeycloakId, String posteRecrutementId);

    boolean existsByCandidatKeycloakIdAndPosteRecrutementId(
            String candidatKeycloakId, String posteRecrutementId);

    List<Application> findByCandidatKeycloakIdOrderByDateCandidatureDesc(String candidatKeycloakId);

    List<Application> findByPosteRecrutementIdOrderByDateCandidatureDesc(String posteRecrutementId);

    List<Application> findByPosteRecrutementIdAndStatut(String posteRecrutementId, ApplicationStatus statut);

    long countByPosteRecrutementId(String posteRecrutementId);
}