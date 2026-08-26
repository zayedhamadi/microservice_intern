package service.recrutement.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.dto.ApplicationDto;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends MongoRepository<Application, String> {

    Page<Application> findByCandidatKeycloakId(String candidatKeycloakId, Pageable pageable);

    Page<Application> findByCandidatKeycloakIdAndStatut(
            String candidatKeycloakId, ApplicationStatus statut, Pageable pageable);

    List<Application> findByCandidatKeycloakId(String candidatKeycloakId);

    Optional<Application> findByCandidatKeycloakIdAndPosteRecrutementId(
            String candidatKeycloakId, String posteRecrutementId);

    List<Application> findByStatut(ApplicationStatus statut);

    boolean existsByCandidatKeycloakIdAndPosteRecrutementId(
            String candidatKeycloakId, String posteRecrutementId);

    List<Application> findByCandidatKeycloakIdOrderByDateCandidatureDesc(String candidatKeycloakId);

    List<Application> findByPosteRecrutementIdOrderByDateCandidatureDesc(String posteRecrutementId);

    List<Application> findByPosteRecrutementIdAndStatut(String posteRecrutementId, ApplicationStatus statut);

    long countByPosteRecrutementId(String posteRecrutementId);

    Optional<ApplicationDto> findByIdApplication(String idApplication);

    List<Application> findByPosteRecrutementId(String posteRecrutementId);

}