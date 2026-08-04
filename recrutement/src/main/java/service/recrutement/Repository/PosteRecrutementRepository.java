package service.recrutement.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import service.recrutement.Entity.PosteRecrutement;

import java.util.List;
import java.util.Optional;


@Repository
public interface PosteRecrutementRepository extends MongoRepository<PosteRecrutement, String> {

    Optional<PosteRecrutement> findByIdPosteRecrutement(String idPosteRecrutement);

    List<PosteRecrutement> findByDepartementNom(String departementNom);

}