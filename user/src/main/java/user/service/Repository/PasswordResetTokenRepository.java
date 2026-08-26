package user.service.Repository;


import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import user.service.Entity.PasswordResetToken;

import java.util.Optional;
@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Transactional
    @Modifying
    void deleteByEmail(String email);
}