package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Application;

@Repository
public interface ApplicationRepository extends MongoRepository<Application, String> {

}
