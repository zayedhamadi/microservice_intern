package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.PosteRecrutement;


@Repository
public interface PosteRecrutementRepository extends MongoRepository<PosteRecrutement, String> { // Long → String

}