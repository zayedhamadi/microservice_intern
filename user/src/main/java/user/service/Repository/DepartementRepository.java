package user.service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import user.service.Entity.Departement;

import java.util.Optional;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Long>{
    Optional<Departement> findByNom(String name);

}
