package service.recrutement.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", contextId = "userNotificationClient")
public interface UserNotificationClient {

    @PostMapping("/internal/notifications/certification")
    void notifyCertification(@RequestBody CertificationNotifyRequest dto);

    record CertificationNotifyRequest(String keycloakId, String titre, String action) {
    }
}