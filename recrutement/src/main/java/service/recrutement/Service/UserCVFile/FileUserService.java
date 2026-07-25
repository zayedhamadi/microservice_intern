package service.recrutement.Service.UserCVFile;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import service.recrutement.Entity.FileUser;
import service.recrutement.Repository.FileUserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileUserService {

    FileUserRepository repository;
    private static final long MAX_CV_SIZE = 5L * 1024 * 1024;

    public void validateCv(MultipartFile file) {
        if (file.isEmpty()) throw new RuntimeException("Fichier vide");
        if (!"application/pdf".equals(file.getContentType())) throw new RuntimeException("Le CV doit être un PDF");
        if (file.getSize() > MAX_CV_SIZE) throw new RuntimeException("CV trop volumineux (max 5MB)");
    }

    public FileUser uploadOrUpdateCv(String keycloakId, byte[] cv, String fileName) {
        FileUser fileUser = repository.findByKeycloakId(keycloakId)
                .orElse(FileUser.builder().keycloakId(keycloakId).build());
        fileUser.setCvUser(cv);
        fileUser.setCvFileName(fileName);
        return repository.save(fileUser);
    }

    public Optional<FileUser> findCv(String keycloakId) {
        return repository.findByKeycloakId(keycloakId);
    }

    public void deleteCv(String keycloakId) {
        repository.findByKeycloakId(keycloakId).ifPresent(repository::delete);
    }
}