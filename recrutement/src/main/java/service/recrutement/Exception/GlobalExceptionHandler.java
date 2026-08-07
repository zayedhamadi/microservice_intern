package service.recrutement.Exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PosteRecrutementNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(PosteRecrutementNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(CandidatureNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(CandidatureNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PosteFermeException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(PosteFermeException e) {
        return build(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({CandidatureExistanteException.class, DuplicateKeyException.class})
    public ResponseEntity<Map<String, Object>> handleDuplicate(RuntimeException e) {
        return build(HttpStatus.CONFLICT, "Vous avez déjà postulé à ce poste");
    }

//    @ExceptionHandler(CvRequisException.class)
//    public ResponseEntity<Map<String, Object>> handleBadRequest(CvRequisException e) {
//        return build(HttpStatus.BAD_REQUEST, e.getMessage());
//    }

    @ExceptionHandler(TransitionStatutInvalideException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(TransitionStatutInvalideException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(AccesNonAutoriseException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccesNonAutoriseException e) {
        return build(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(RuntimeException e) {
        log.error("Erreur non gérée : {}", e.getMessage(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}