package service.recrutement.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Certification;

import java.util.List;

@Repository
public interface CertificationRepository
        extends MongoRepository<Certification, String> {

    Page<Certification> findByKeycloakId(
            String keycloakId,
            Pageable pageable
    );

    Page<Certification>
    findByKeycloakIdAndTitreContainingIgnoreCase(
            String keycloakId,
            String titre,
            Pageable pageable
    );

    List<Certification> findByKeycloakId(
            String keycloakId
    );

    long countByKeycloakId(
            String keycloakId
    );
}