package user.service.Serivce.Authorization;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.service.Entity.RevokedToken;
import user.service.Repository.RevokedTokenRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional
    public void blacklist(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null) return;
        if (revokedTokenRepository.existsByJti(jti)) return; // déjà présent

        LocalDateTime expiry = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault());
        RevokedToken revoked = RevokedToken.builder()
                .jti(jti)
                .expiresAt(expiry)
                .build();

        revokedTokenRepository.save(revoked);
        log.info("Token {} révoqué jusqu'à {}", jti, expiry);
    }

    public boolean isBlacklisted(String jti) {
        return jti != null && revokedTokenRepository.existsByJti(jti);
    }
}
