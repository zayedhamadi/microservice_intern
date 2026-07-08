package user.service.Exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException e) {
        log.error("RuntimeException non gérée : ", e);
        return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "Erreur inattendue"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception e) {
        log.error("Exception non gérée (500) : ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Erreur serveur : " + e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "aucun détail"
        ));
    }
}