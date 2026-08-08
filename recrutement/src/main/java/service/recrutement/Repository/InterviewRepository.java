package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.Interview;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends MongoRepository<Interview, String> {

    List<Interview> findByApplicationIdOrderByDateCreationDesc(String applicationId);

    Optional<Interview> findByApplicationIdAndType(String applicationId, InterviewType type);
}