package user.service.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import user.service.Entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.num_Tel = :numTel")
    Optional<User> findByNumTel(@Param("numTel") Integer numTel);
    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakId(String keycloakId);
    boolean existsByEmail(String email);
    boolean existsByKeycloakId(String keycloakId);
}
