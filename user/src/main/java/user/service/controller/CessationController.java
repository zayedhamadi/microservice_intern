package user.service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import user.service.Dto.CessationDTO;
import user.service.Mail.UserEmailService;
import user.service.Serivce.Admin.CessationService;

@Slf4j
@RestController
@RequestMapping("/cessation")
@RequiredArgsConstructor
public class CessationController {

    private final CessationService cessationService;
    private final UserEmailService userEmailService;

    @PostMapping("/users/{id}/cesser")
    public ResponseEntity<Void> cesserUser(@PathVariable Long id, @RequestBody CessationDTO dto) {
        try {
            cessationService.cesserUser(id, dto);
        } catch (RuntimeException e) {
            log.error("Erreur cessation user {} : {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }

        this.cessationService.notifyAndEmail(id, dto.getMotifCessation());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/reactiver")
    public ResponseEntity<Void> reactiverUser(@PathVariable Long id){

        try {
            cessationService.reactiverUser(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Erreur réactivation user {} : {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}