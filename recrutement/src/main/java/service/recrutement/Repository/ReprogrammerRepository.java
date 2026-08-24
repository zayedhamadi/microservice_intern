package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Enum.DemandeReportStatus;
import service.recrutement.Entity.Reprogrammer;

import java.util.List;

@Repository
public interface ReprogrammerRepository extends MongoRepository<Reprogrammer, String> {

    List<Reprogrammer> findByInterviewIdOrderByDateCreationDesc(String interviewId);

    List<Reprogrammer> findByDemandeurKeycloakIdOrderByDateCreationDesc(String demandeurKeycloakId);

    List<Reprogrammer> findByCibleKeycloakIdAndStatutOrderByDateCreationDesc(
            String cibleKeycloakId, DemandeReportStatus statut);

    List<Reprogrammer> findByStatutOrderByDateCreationDesc(DemandeReportStatus statut);

    boolean existsByInterviewIdAndStatut(String interviewId, DemandeReportStatus statut);
}