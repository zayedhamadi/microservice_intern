package service.recrutement.Service;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import service.recrutement.Entity.FileUser;
import service.recrutement.Repository.FileUserRepository;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserFinderService {

    FileUserRepository repository;

    public FileUser bykeycloakId(String keycloakId) {
        return repository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User non trouvé avec keycloakId : " + keycloakId));
    }


    

}