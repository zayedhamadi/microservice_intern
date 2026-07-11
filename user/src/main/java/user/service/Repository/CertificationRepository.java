package user.service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import user.service.Entity.Certification;

import java.util.List;
@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

    List<Certification> findByUserId(Long userId);
}
