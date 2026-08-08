package user.service.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${internal.api.key}")
    private String expectedApiKey;

    private static final String HEADER_NAME = "X-Internal-Api-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/internal/")) {
            String providedKey = request.getHeader(HEADER_NAME);
            if (providedKey == null || !providedKey.equals(expectedApiKey)) {
                log.warn("Appel /internal/** refusé : clé API manquante ou invalide depuis {}",
                        request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès interne non autorisé");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}