package service.recrutement.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.dto.*;
import service.recrutement.Security.JwtExtractService;
import service.recrutement.Service.CalendarInterviewService;
import service.recrutement.Service.InterviewService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InterviewController {
    private final InterviewService interviewService;
    private final CalendarInterviewService calendarInterviewService;
    private final JwtExtractService jwtExtractService;

    @GetMapping("/interviews")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<List<CalendarInterviewDto>> getAllCalendar() {
        return ResponseEntity.ok(calendarInterviewService.getAll());
    }

    @GetMapping("/rh/api/entretiens")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<List<CalendarInterviewDto>> getAllEntretiensForCalendar() {
        return ResponseEntity.ok(interviewService.getAllRecrutementAsCalendar());
    }

    @GetMapping("/interviews/{id}")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<CalendarInterviewDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(calendarInterviewService.getById(id));
    }

    @PostMapping("/interviews")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<CalendarInterviewDto> create(@Valid @RequestBody CalendarInterviewDto dto) {
        return ResponseEntity.ok(calendarInterviewService.create(dto));
    }

    @PutMapping("/interviews/{id}")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<CalendarInterviewDto> update(@PathVariable String id, @Valid @RequestBody CalendarInterviewDto dto) {
        return ResponseEntity.ok(calendarInterviewService.update(id, dto));
    }

    @DeleteMapping("/interviews/{id}")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        calendarInterviewService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/interviews/stats")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<List<CalendarInterviewStatsDto>> stats() {
        return ResponseEntity.ok(calendarInterviewService.getStats());
    }

    @PostMapping("/rh/api/candidatures/{id}/entretiens/rh-initial")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<InterviewDto> rhInitial(@PathVariable String id, @Valid @RequestBody PlanifierEntretienDto dto, Authentication auth) {
        return ResponseEntity.ok(interviewService.planifierEntretien(id, InterviewType.RH_INITIAL, dto, jwtExtractService.extractKeycloakId(auth)));
    }

    @PostMapping("/rh/api/candidatures/{id}/entretiens/technique")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<InterviewDto> tech(@PathVariable String id, @Valid @RequestBody PlanifierEntretienDto dto, Authentication auth) {
        return ResponseEntity.ok(interviewService.planifierEntretien(id, InterviewType.TECHNIQUE, dto, jwtExtractService.extractKeycloakId(auth)));
    }

    @PostMapping("/rh/api/candidatures/{id}/entretiens/rh-final")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<InterviewDto> rhFinal(@PathVariable String id, @Valid @RequestBody PlanifierEntretienDto dto, Authentication auth) {
        return ResponseEntity.ok(interviewService.planifierEntretien(id, InterviewType.RH_FINAL, dto, jwtExtractService.extractKeycloakId(auth)));
    }

    @PatchMapping("/rh/api/entretiens/{interviewId}/resultat")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<InterviewDto> resultat(@PathVariable String interviewId, @Valid @RequestBody ResultatEntretienDto dto, Authentication auth) {
        return ResponseEntity.ok(interviewService.enregistrerResultat(interviewId, dto, jwtExtractService.extractKeycloakId(auth)));
    }

    @GetMapping("/rh/api/candidatures/{id}/entretiens")
    @PreAuthorize("hasAnyRole('CANDIDAT','RH','EMPLOYEE')")
    public ResponseEntity<List<InterviewDto>> getEnt(@PathVariable String id, Authentication auth) {
        boolean rh = jwtExtractService.hasRole(auth, "ROLE_RH") || jwtExtractService.hasRole(auth, "ROLE_EMPLOYEE");
        return ResponseEntity.ok(interviewService.getEntretiensPourCandidature(id, jwtExtractService.extractKeycloakId(auth), rh));
    }
}