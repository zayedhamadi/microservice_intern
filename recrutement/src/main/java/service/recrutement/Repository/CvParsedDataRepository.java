package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.CvParsedData;


@Repository
public interface CvParsedDataRepository extends MongoRepository<CvParsedData, String> {
}