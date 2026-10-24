package service.recrutement.Config;

import org.springframework.context.annotation.Configuration;

// CORS est géré de manière centralisée par le gateway.
// Ne PAS définir de bean CorsFilter ici : cela provoquait un header
// Access-Control-Allow-Origin dupliqué (même bug déjà corrigé sur user-service).
@Configuration
public class CorsConfig {
}
