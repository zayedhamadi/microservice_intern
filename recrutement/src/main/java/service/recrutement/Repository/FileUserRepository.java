package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import service.recrutement.Entity.FileUser;

import java.util.Optional;

public interface FileUserRepository extends MongoRepository<FileUser, String> {
    Optional<FileUser> findByKeycloakId(String keycloakId);
}