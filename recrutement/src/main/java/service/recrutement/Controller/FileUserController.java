package service.recrutement.Controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import service.recrutement.Entity.FileUser;
import service.recrutement.Entity.dto.fileUSerDto;
import service.recrutement.Service.UserCVFile.FileUserService;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/cv")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileUserController {

    FileUserService fileUserService;



    @PostMapping
    public ResponseEntity<Void> uploadOrUpdateCv(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam("file") MultipartFile file) throws IOException {
        this.fileUserService.validateCv(file);
        fileUserService.uploadOrUpdateCv(jwt.getSubject(), file.getBytes(), file.getOriginalFilename());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<fileUSerDto> getMyCv(@AuthenticationPrincipal Jwt jwt) {
        return fileUserService.findCv(jwt.getSubject())
                .map(fu -> ResponseEntity.ok(fileUSerDto.builder()
                        .exists(true)
                        .cvFileName(fu.getCvFileName())
                        .cvBase64("data:application/pdf;base64," + Base64.getEncoder().encodeToString(fu.getCvUser()))
                        .build()))
                .orElse(ResponseEntity.ok(fileUSerDto.builder().exists(false).build()));
    }

    // Pour un vrai téléchargement de fichier (bytes bruts)
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadMyCv(@AuthenticationPrincipal Jwt jwt) {
        FileUser fileUser = fileUserService.findCv(jwt.getSubject())
                .orElseThrow(() -> new RuntimeException("Aucun CV trouvé"));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileUser.getCvFileName() + "\"")
                .body(fileUser.getCvUser());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMyCv(@AuthenticationPrincipal Jwt jwt) {
        fileUserService.deleteCv(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }


}