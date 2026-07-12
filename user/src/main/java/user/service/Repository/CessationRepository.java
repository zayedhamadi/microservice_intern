package user.service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import user.service.Entity.Cessation;

@Repository
public interface CessationRepository extends JpaRepository<Cessation, Long> {
}