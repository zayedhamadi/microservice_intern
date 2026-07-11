package user.service.Serivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import user.service.Repository.RevokedTokenRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final RevokedTokenRepository revokedTokenRepository;

    // Tous les jours à 3h du matin
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        int deleted = revokedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("{} tokens révoqués expirés supprimés", deleted);
    }
}