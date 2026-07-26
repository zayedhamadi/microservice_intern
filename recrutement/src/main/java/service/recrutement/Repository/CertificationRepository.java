package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Certification;

import java.util.List;

@Repository
public interface CertificationRepository extends MongoRepository<Certification, String> { // Long → String

    List<Certification> findByKeycloakId(String keycloakId);
}