package user.service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import user.service.Entity.User;
import user.service.Serivce.UserCommun.UserFinderService;
import user.service.Serivce.WebSocket.AdminRealtimeService;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final AdminRealtimeService adminRealtimeService;
    private final UserFinderService userFinderService;

    @PostMapping("/certification")
    public void notifyCertification(@RequestBody CertificationNotifyDto dto) {
        User user = userFinderService.getUserByKeycloakId(dto.keycloakId());
        adminRealtimeService.notifyCertification(user.getPrenom(), user.getNom(), dto.titre(), dto.action());
    }

    public record CertificationNotifyDto(String keycloakId, String titre, String action) {
    }
}