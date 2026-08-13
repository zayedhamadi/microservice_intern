package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface enregistrerPosteRecrutement extends MongoRepository<enregistrerPosteRecrutement,String> {
}
