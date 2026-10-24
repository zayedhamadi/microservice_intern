package user.service.Security;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    private static final String[] PUBLIC_URLS = {
            "/auth/register",
            "/auth/login",
            "/auth/google/url",
            "/auth/google/sync",
            "/auth/google/callback",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/internal/**",
            "/ws-admin/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/uploads/**"
    };

    JwtAuthConverter jwtAuthConverter;
    UserSyncFilter userSyncFilter;
    JwtBlacklistFilter jwtBlacklistFilter;
    InternalApiKeyFilter internalApiKeyFilter;

   @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .requestMatchers("/cv/**").authenticated()
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
            )
            .addFilterBefore(internalApiKeyFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(userSyncFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(jwtBlacklistFilter, UserSyncFilter.class);

    return http.build();
}
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setRequestProperty("Connection", "close");
            }
        };
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(500000);

        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
