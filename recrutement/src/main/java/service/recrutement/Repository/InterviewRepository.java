package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.Interview;



@Repository
public interface InterviewRepository extends MongoRepository<Interview, String> {

}