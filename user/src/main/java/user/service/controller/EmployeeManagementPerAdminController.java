package user.service.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import user.service.Dto.createUserPerAdminDto;
import user.service.Entity.User;
import user.service.Serivce.Admin.EmployeeManagement;
import user.service.Serivce.Admin.UserStatistics;
import user.service.Serivce.WebSocket.ActivityService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PreAuthorize("hasAnyRole('EMPLOYEE','RH')")
@Slf4j
@RestController
@RequestMapping("/EmployeeManagementController")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeManagementPerAdminController {

    EmployeeManagement employeeManagement;
    UserStatistics userStatistics;
    ActivityService activityService;

    @PostMapping("/admin/register")
    public ResponseEntity<?> register(@Valid @RequestBody createUserPerAdminDto request) {
        try {
            User user = employeeManagement.register(request);
            employeeManagement.postRegister(user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Compte créé avec succès",
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "matricule", user.getMatricule()
            ));
        } catch (RuntimeException e) {
            log.error("Erreur register (admin) : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== ENDPOINTS POUR LES ACTIVITÉS ====================

    /**
     * Récupère les activités récentes (avec nettoyage optionnel des anciennes activités).
     *
     * @param limit      Nombre maximal d'activités à retourner.
     * @param daysToKeep Nombre de jours à conserver (optionnel, par défaut 30).
     * @return Liste des activités récentes.
     */
    @GetMapping("/activities")
    public ResponseEntity<List<Map<String, Object>>> getActivities(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "30") int daysToKeep) {
        return ResponseEntity.ok(activityService.getRecentAndCleanOld(limit, daysToKeep));
    }

    /**
     * Supprime toutes les activités.
     */
    @DeleteMapping("/activities")
    public ResponseEntity<Void> clearActivities() {
        activityService.clear();
        return ResponseEntity.noContent().build();
    }

    /**
     * Supprime les activités anciennes (plus vieilles que `days` jours).
     *
     * @param days Nombre de jours à conserver.
     * @return Réponse vide.
     */
    @DeleteMapping("/activities/old")
    public ResponseEntity<Void> cleanOldActivities(@RequestParam(defaultValue = "30") int days) {
        activityService.deleteOldActivities(days);
        return ResponseEntity.noContent().build();
    }

    // ==================== AUTRES ENDPOINTS (INCHANGÉS) ====================

    @GetMapping("/stats/monthly-registrations")
    public ResponseEntity<Map<String, List<Integer>>> getMonthlyRegistrations() {
        return ResponseEntity.ok(userStatistics.getMonthlyRegistrations());
    }

    @GetMapping("/test-db")
    public ResponseEntity<Map<String, Object>> testDb() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("status", "OK");
            result.put("countUsers", userStatistics.getCountUsers());
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            result.put("cause", e.getCause() != null ? e.getCause().getMessage() : "null");
            log.error("Test DB error", e);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/monthly-inscr-vs-cessation")
    public ResponseEntity<Map<String, List<Integer>>> getMonthlyInscrVsCessation() {
        return ResponseEntity.ok(userStatistics.getMonthlyInscrVsCessation());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userStatistics.getUserById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/usersAdmin/{id}")
    public ResponseEntity<Map<String, Object>> getUserByIddAdmin(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userStatistics.findUserDetailByIdAdmin(id));
        } catch (RuntimeException e) {
            log.warn("Utilisateur admin introuvable ou erreur requête (id={}) : {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(@RequestParam String q) {
        if (q == null || q.isBlank()) return ResponseEntity.badRequest().build();
        List<Map<String, Object>> users = userStatistics.searchUsers(q.trim());
        return users.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(users);
    }

    @GetMapping("/allActiveUsers/paged")
    public ResponseEntity<Map<String, Object>> getAllActiveUsersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        int offset = page * size;
        List<Map<String, Object>> users = userStatistics.getAllActiveUsersPaged(offset, size);
        Long total = userStatistics.countAllActiveUsers();
        Map<String, Object> response = new HashMap<>();
        response.put("content", users);
        response.put("totalElements", total);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        response.put("page", page);
        response.put("size", size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/status-by-role")
    public ResponseEntity<Map<String, Map<String, Long>>> getStatusByRole() {
        Map<String, Map<String, Long>> data = userStatistics.getStatusByRole();
        return data.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(data);
    }

    @GetMapping("/stats/genre-by-role")
    public ResponseEntity<Map<String, Map<String, Long>>> getGenreByRole() {
        Map<String, Map<String, Long>> data = userStatistics.getGenreByRole();
        return data.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(data);
    }

    @GetMapping("/getStatisticsGenreUsers")
    public ResponseEntity<Map<String, Long>> getStatisticsGenreUsers(Authentication authentication) {
        log.info("Roles: {}", authentication.getAuthorities());
        Map<String, Long> map = userStatistics.getStatisticsUserGener();
        return map.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(map);
    }

    @GetMapping("/stats/users")
    public ResponseEntity<Map<String, Object>> getUsersStats() {
        return ResponseEntity.ok(userStatistics.getUsersStats());
    }

    @GetMapping("/stats/rh")
    public ResponseEntity<Map<String, Object>> getRHStats() {
        return ResponseEntity.ok(userStatistics.getRHStats());
    }

    @GetMapping("/stats/employees")
    public ResponseEntity<Map<String, Object>> getEmployeeStats() {
        return ResponseEntity.ok(userStatistics.getEmployeeStats());
    }

    @GetMapping("/stats/candidats")
    public ResponseEntity<Map<String, Object>> getCandidatsStats() {
        return ResponseEntity.ok(userStatistics.getCandidatsStats());
    }

    @GetMapping("/stats/inactifs")
    public ResponseEntity<Map<String, Object>> getInactifsStats() {
        return ResponseEntity.ok(userStatistics.getInactifsStats());
    }

    @GetMapping("/last5Users")
    public ResponseEntity<List<Map<String, Object>>> getLast5Users() {
        List<Map<String, Object>> users = userStatistics.getLast5InscriptionUsers();
        return users.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(users);
    }

    @GetMapping("/allActiveUsers")
    public ResponseEntity<List<Map<String, Object>>> getAllActiveUsers() {
        List<Map<String, Object>> users = userStatistics.getAllActiveUsers();
        return users.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(users);
    }

    @GetMapping("/allInactifUsers")
    public ResponseEntity<List<Map<String, Object>>> getAllInactifUsers() {
        List<Map<String, Object>> users = userStatistics.getAllInactifUsers();
        return users.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(users);
    }

    @GetMapping("/getCountUsers")
    public ResponseEntity<?> getCountUsers() {
        return ResponseEntity.ok(userStatistics.getCountUsers());
    }

    @GetMapping("/getCountRHUsers")
    public ResponseEntity<?> getCountRHUsers() {
        return ResponseEntity.ok(userStatistics.getCountRHUsers());
    }

    @GetMapping("/getCountEmployeeUsers")
    public ResponseEntity<?> getCountEmployeeUsers() {
        return ResponseEntity.ok(userStatistics.getCountEmployeeUsers());
    }

    @GetMapping("/getCountCandidatUsers")
    public ResponseEntity<?> getCountCandidatUsers() {
        return ResponseEntity.ok(userStatistics.getCountCandidatUsers());
    }

    @GetMapping("/getCountInactifUsers")
    public ResponseEntity<?> getCountInactifUsers() {
        return ResponseEntity.ok(userStatistics.getCountInactifUsers());
    }
}