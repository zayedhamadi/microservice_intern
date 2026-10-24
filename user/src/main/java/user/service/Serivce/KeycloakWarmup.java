

package user.service.Serivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakWarmup {

    private final RestTemplate restTemplate;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        try {
            restTemplate.getForEntity(
                    serverUrl + "/realms/master",
                    String.class
            );

            log.info("Warm-up Keycloak effectué avec succès au démarrage");

        } catch (Exception e) {
            log.warn(
                    "Warm-up Keycloak échoué (non bloquant) : {}",
                    e.getMessage()
            );
        }
    }
}

