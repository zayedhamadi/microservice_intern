package user.service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import user.service.Serivce.Authorization.TokenBlacklistService;

@RestController
@RequestMapping("/internal/tokens")
@RequiredArgsConstructor
public class InternalTokenController {

    private final TokenBlacklistService tokenBlacklistService;

    @GetMapping("/{jti}/blacklisted")
    public boolean isBlacklisted(@PathVariable String jti) {
        return tokenBlacklistService.isBlacklisted(jti);
    }
}