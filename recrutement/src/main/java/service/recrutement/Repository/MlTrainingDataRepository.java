package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.MlTrainingData;


@Repository
public interface MlTrainingDataRepository extends MongoRepository<MlTrainingData, String> {

}