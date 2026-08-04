package user.service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import user.service.Entity.Departement;

import java.util.Optional;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Long>{
    @Query("SELECT d FROM Departement d WHERE TRIM(d.nom) = TRIM(:nom)")
    Optional<Departement> findByNom(@Param("nom") String nom);

}
