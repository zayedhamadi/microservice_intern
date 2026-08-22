package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Enum.InterviewSource;
import service.recrutement.Entity.Enum.InterviewStatus;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.Interview;

import java.util.List;

@Repository
public interface InterviewRepository extends MongoRepository<Interview, String> {
    List<Interview> findByRecruteurKeycloakIdOrderByDateEntretienAsc(String recruteurKeycloakId);

    List<Interview> findByRecruteurKeycloakIdAndTypeAndSource(
            String recruteurKeycloakId, InterviewType type, InterviewSource source);

    List<Interview> findBySource(InterviewSource source);

    List<Interview> findByApplicationIdOrderByDateCreationDesc(String applicationId);

    List<Interview> findByTypeAndSource(InterviewType type, InterviewSource source);

    boolean existsByApplicationIdAndTypeAndStatutIn(
            String applicationId, InterviewType type, List<InterviewStatus> statuts);
}